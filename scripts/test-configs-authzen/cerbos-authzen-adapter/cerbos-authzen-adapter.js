// cerbos-authzen-adapter.js
//
// Minimal AuthZEN adapter that sits in front of Cerbos (the PDP) and works
// around a v0.55.0 limitation: the AuthZEN evaluation endpoints forward
// only `action.name` to the engine and drop `action.properties`, so a policy
// cannot see flags such as the certification fixture's `delete` `soft` flag.
//
// This adapter lifts each request's effective `action.properties` onto the
// resource's `properties`, so the policy can read them as `R.attr.*`
// (e.g. `request.resource.attr.soft`). It also strips unknown structural fields
// from evaluation requests before forwarding, because Cerbos rejects them
// (DiscardUnknown:false) with HTTP 400; the free-form `properties`/`context`
// objects pass through untouched. Everything else is passed through unchanged.
// It also echoes an inbound `X-Request-ID` back on the response (Cerbos does
// not); when the client sends none, none is added. On the evaluation endpoints
// non-POST methods are rejected with HTTP 405 (OPTIONS passes through for CORS)
// and non-JSON Content-Types with HTTP 415, rather than being forwarded. It also sets `X-Forwarded-Proto: https` so the AuthZEN discovery
// document advertises https:// URLs.
//
// Config (env):
//   CERBOS_URL             upstream Cerbos base URL     (default https://cerbos:3592)
//   LISTEN_PORT            port this proxy listens on   (default 9090)
//   TLS_CERT               path to a PEM cert; serve HTTPS when set with TLS_KEY
//   TLS_KEY                path to the matching PEM private key (default unset)
//   FORWARDED_PROTO        value for X-Forwarded-Proto  (default https)
//   FORWARDED_HOST         value for X-Forwarded-Host   (default unset)
//   FORWARDED_PORT         port to fold into the host when it lacks one and the
//                          port is non-default for the scheme (default unset)
//   TRUST_FORWARDED_HEADERS  if truthy, an inbound X-Forwarded-Proto/Host/Port wins
//                            and FORWARDED_* is only a fallback when it is
//                            absent; otherwise FORWARDED_* always overrides
//                            (default false)
//
// Cerbos builds the /.well-known/authzen-configuration URLs from these two
// headers: scheme from X-Forwarded-Proto (only "https" flips it off http), and
// host:port from X-Forwarded-Host verbatim (it ignores X-Forwarded-Port). This
// proxy therefore folds a separately-supplied port (inbound X-Forwarded-Port or
// FORWARDED_PORT) into the host when the host has no port of its own, dropping it
// when it is the scheme's default (80/http, 443/https). Set FORWARDED_HOST to the
// address clients actually reach the PDP on; leave it unset to fall back to the
// Host/:authority header the proxy forwards upstream. When another reverse proxy
// (e.g. nginx) sits in front and already sets these headers, set
// TRUST_FORWARDED_HEADERS=true so its values are preserved.

const http = require("http");
const https = require("https");
const fs = require("fs");
const { URL } = require("url");

const CERBOS_URL = process.env.CERBOS_URL || "https://cerbos:3592";
const LISTEN_PORT = Number(process.env.LISTEN_PORT || 9090);
// When both TLS_CERT and TLS_KEY point at readable PEM files the adapter serves
// HTTPS; otherwise it serves plain HTTP. Generate a local pair with
// ./generate-certs.sh (writes certs/cert.pem and certs/key.pem).
const TLS_CERT = process.env.TLS_CERT || "";
const TLS_KEY = process.env.TLS_KEY || "";
const FORWARDED_PROTO = process.env.FORWARDED_PROTO || "https";
const FORWARDED_HOST = process.env.FORWARDED_HOST || "";
const FORWARDED_PORT = process.env.FORWARDED_PORT || "";
const TRUST_FORWARDED_HEADERS = /^(1|true|yes)$/i.test(
	process.env.TRUST_FORWARDED_HEADERS || "",
);

const upstream = new URL(CERBOS_URL);
const upstreamClient = upstream.protocol === "http:" ? http : https;

// Shallow-merge an action's properties onto a resource's properties.
function liftActionProps(action, resource) {
	const res = { ...(resource || {}) };
	if (action && action.properties) {
		res.properties = { ...(res.properties || {}), ...action.properties };
	}
	return res;
}

// Whitelist the known structural fields of each AuthZEN entity/message, dropping
// any others. Cerbos deserializes with DiscardUnknown:false, so an unrecognized
// structural field (e.g. subject.foo, or a stray top-level field) makes it reject
// the whole request with HTTP 400. `properties` and `context` are google.protobuf
// .Struct — arbitrary keys inside them are valid, so their values pass untouched.
const KEEP = (obj, keys) => {
	if (!obj || typeof obj !== "object") return obj;
	const out = {};
	for (const k of keys) if (k in obj) out[k] = obj[k];
	return out;
};
const cleanSubject = (s) => KEEP(s, ["type", "id", "properties"]);
const cleanResource = (r) => KEEP(r, ["type", "id", "properties"]);
const cleanAction = (a) => KEEP(a, ["name", "properties"]);
// protojson accepts both the proto field name and its camelCase form.
const cleanOptions = (o) =>
	KEEP(o, ["evaluations_semantic", "evaluationsSemantic"]);

// Rewrite a parsed AuthZEN request body: strip unknown structural fields, then
// lift action.properties onto resource.properties so the flags reach the policy
// as resource attributes. Handles both the single and batch shapes.
function rewrite(body) {
	const b = JSON.parse(body);
	if (!b || typeof b !== "object" || Array.isArray(b)) return body; // let Cerbos judge

	if (Array.isArray(b.evaluations)) {
		// Batch: merge the top-level subject/action/resource/context into every
		// evaluation as defaults; the evaluation's own object shallow-overwrites the
		// top-level one (its keys win). Each evaluation is fully resolved and the
		// now-redundant top-level entity defaults are dropped (options are kept).
		// This also ensures the lifted `soft` isn't lost to Cerbos' own defaulting.
		//
		// Caveat: Cerbos' batch handler groups evaluations by
		// (subject, resourceType, resourceId) and evaluates each group from the
		// FIRST evaluation's resource attributes only (internal/svc/authzen_svc.go
		// v0.55.0). Two evaluations that share a resource id but differ only in
		// attributes — e.g. soft vs hard delete of record-1 — therefore collapse to
		// one decision. This cannot be corrected by rewriting the body. Route such
		// cases through the single /access/v1/evaluation endpoint instead.
		const defSubject = b.subject ? cleanSubject(b.subject) : undefined;
		const defResource = b.resource ? cleanResource(b.resource) : undefined;
		const defAction = b.action ? cleanAction(b.action) : undefined;
		const defContext = "context" in b ? b.context : undefined;

		// Merge a top-level default object with an evaluation's object; the
		// evaluation's keys overwrite the default's. Returns undefined only when
		// neither side supplied the object, so it is omitted rather than sent empty.
		const mergeDefault = (def, override) =>
			def === undefined && override === undefined
				? undefined
				: { ...(def || {}), ...(override || {}) };

		const out = {};
		if (b.options) out.options = cleanOptions(b.options);
		out.evaluations = b.evaluations.map((ev) => {
			const evObj = ev && typeof ev === "object" ? ev : {};
			const subject = mergeDefault(defSubject, cleanSubject(evObj.subject));
			const action = mergeDefault(defAction, cleanAction(evObj.action));
			const resource = mergeDefault(defResource, cleanResource(evObj.resource));
			const context = mergeDefault(
				defContext,
				"context" in evObj ? evObj.context : undefined,
			);

			const node = {};
			if (subject !== undefined) node.subject = subject;
			if (action !== undefined) node.action = action;
			// Lift action.properties onto the (merged) resource; also create a
			// resource to carry them when the request supplied none of its own.
			if (resource !== undefined || (action && action.properties)) {
				node.resource = liftActionProps(action, resource);
			}
			if (context !== undefined) node.context = context;
			return node;
		});
		return JSON.stringify(out);
	}

	// Single evaluation.
	const out = {};
	if (b.subject) out.subject = cleanSubject(b.subject);
	if (b.action) out.action = cleanAction(b.action);
	if ("context" in b) out.context = b.context; // Struct, untouched
	out.resource = liftActionProps(out.action, cleanResource(b.resource));
	return JSON.stringify(out);
}

// True when a request to the /access/v1/evaluations (batch) endpoint is really a
// single evaluation expressed with top-level subject/action/resource and no
// populated evaluations array — in which case it is routed to the singular
// /access/v1/evaluation endpoint (Cerbos' batch endpoint would otherwise return
// no decisions for it).
function collapsibleToSingle(b) {
	if (!b || typeof b !== "object") return false;
	const hasTopLevel = Boolean(b.subject && b.action && b.resource);
	const noEvaluations =
		!("evaluations" in b) ||
		(Array.isArray(b.evaluations) && b.evaluations.length === 0);
	return hasTopLevel && noEvaluations;
}

// Accept application/json, application/json+pretty, any structured +json suffix,
// and an optional charset/other parameter after ";".
function isJsonContentType(ct) {
	if (!ct) return false;
	const media = ct.split(";")[0].trim().toLowerCase();
	return (
		media === "application/json" ||
		media === "application/json+pretty" ||
		media.endsWith("+json")
	);
}

const handler = (req, res) => {
	const chunks = [];
	req.on("data", (c) => chunks.push(c));
	req.on("end", () => {
		const raw = Buffer.concat(chunks).toString("utf8");

		// Only rewrite POSTs to the evaluation endpoints; pass everything else
		// (health, discovery, GETs) straight through.
		let outBody = raw;
		const isEvalPath = /^\/access\/v1\/evaluations?(\?|$)/.test(req.url);
		const isEvaluationsPath = /^\/access\/v1\/evaluations(\?|$)/.test(req.url);
		const isEval = isEvalPath && req.method === "POST";
		let forwardUrl = req.url;

		// The evaluation endpoints are POST-only; reject other methods with 405.
		// OPTIONS is passed through so Cerbos' CORS preflight handling still works.
		if (isEvalPath && req.method !== "POST" && req.method !== "OPTIONS") {
			const h = { allow: "POST", "content-type": "application/json" };
			const rid = req.headers["x-request-id"];
			if (rid) h["x-request-id"] = rid;
			res.writeHead(405, h);
			res.end(JSON.stringify({ error: "method not allowed; use POST" }));
			return;
		}

		// The evaluation endpoints take a JSON body; reject anything else with 415
		// rather than forwarding it (Cerbos would wildcard-accept it as JSON).
		if (isEval && !isJsonContentType(req.headers["content-type"])) {
			const h = { "content-type": "application/json" };
			const rid = req.headers["x-request-id"];
			if (rid) h["x-request-id"] = rid;
			res.writeHead(415, h);
			res.end(
				JSON.stringify({ error: "unsupported media type; expected application/json" }),
			);
			return;
		}

		if (isEval && raw) {
			try {
				let body = raw;
				// A /evaluations (batch) request that carries only top-level
				// subject/action/resource and no populated evaluations array is a single
				// evaluation — route it to the singular /evaluation endpoint so Cerbos
				// actually evaluates it instead of returning an empty batch result.
				if (isEvaluationsPath) {
					const parsed = JSON.parse(raw);
					if (collapsibleToSingle(parsed)) {
						forwardUrl = req.url.replace(/\/evaluations(\?|$)/, "/evaluation$1");
						delete parsed.evaluations;
						body = JSON.stringify(parsed);
					}
				}
				outBody = rewrite(body);
			} catch (e) {
				// Malformed JSON — let Cerbos return the real validation error.
				outBody = raw;
			}
		}

		const target = new URL(forwardUrl, upstream);
		const headers = { ...req.headers };
		headers["host"] = upstream.host; // always required to route upstream

		// Choose between an inbound header and the configured value. With
		// TRUST_FORWARDED_HEADERS an inbound value from a front reverse proxy wins
		// and config is only a fallback; otherwise config always overrides.
		const pick = (inbound, config) =>
			TRUST_FORWARDED_HEADERS ? inbound || config : config || inbound;

		const scheme = pick(headers["x-forwarded-proto"], FORWARDED_PROTO);
		if (scheme) headers["x-forwarded-proto"] = scheme;

		// Cerbos reads the advertised port ONLY from X-Forwarded-Host and ignores
		// X-Forwarded-Port. An nginx front proxy typically sends the host without a
		// port and the port separately, so fold the port into the host here — but
		// omit it when it is the scheme's default (80 for http, 443 for https), and
		// never clobber a port the host already carries.
		let host = pick(headers["x-forwarded-host"], FORWARDED_HOST);
		if (host && !/:\d+$/.test(host)) {
			const port = pick(headers["x-forwarded-port"], FORWARDED_PORT);
			const isDefault =
				(scheme === "https" && port === "443") ||
				(scheme === "http" && port === "80");
			if (port && !isDefault) host = `${host}:${port}`;
		}
		if (host) headers["x-forwarded-host"] = host;

		// Cerbos does not echo a request id. When the client supplies one, echo it
		// back on the response (success and error paths alike) for correlation;
		// when absent, add nothing. It is forwarded upstream as part of req.headers.
		const requestId = req.headers["x-request-id"];

		if (outBody) headers["content-length"] = Buffer.byteLength(outBody);

		const upReq = upstreamClient.request(
			target,
			{
				method: req.method,
				headers,
				rejectUnauthorized: false, // self-signed dev cert
			},
			(upRes) => {
				const respHeaders = { ...upRes.headers };
				if (requestId) respHeaders["x-request-id"] = requestId;
				res.writeHead(upRes.statusCode || 502, respHeaders);
				upRes.pipe(res);
			},
		);
		upReq.on("error", (e) => {
			const errHeaders = { "content-type": "text/plain" };
			if (requestId) errHeaders["x-request-id"] = requestId;
			res.writeHead(502, errHeaders);
			res.end(`adapter upstream error: ${e.message}`);
		});
		if (outBody) upReq.write(outBody);
		upReq.end();
	});
};

const useTls = Boolean(TLS_CERT && TLS_KEY);
const server = useTls
	? https.createServer(
		{ cert: fs.readFileSync(TLS_CERT), key: fs.readFileSync(TLS_KEY) },
		handler,
	)
	: http.createServer(handler);

server.listen(LISTEN_PORT, () => {
	const fh = FORWARDED_HOST || "(from Host header)";
	const mode = TRUST_FORWARDED_HEADERS ? "inbound-wins" : "config-wins";
	const scheme = useTls ? "https" : "http";
	console.log(
		`Cerbos AuthZEN adapter listening on ${scheme}://:${LISTEN_PORT} -> ${CERBOS_URL} ` +
		`(advertising ${FORWARDED_PROTO}://${fh}, ${mode})`,
	);
});
