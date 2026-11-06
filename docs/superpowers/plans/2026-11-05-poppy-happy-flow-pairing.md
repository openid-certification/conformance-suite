# Poppy 0.4 Happy Flow (Company and Agent sides, paired in CI) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Two new test plans, `poppy-company-test-plan` (the suite plays a Personal Agent against a Company) and `poppy-agent-test-plan` (the suite plays a Company against a Personal Agent), each with one happy-flow module for Personal Agent Protocol draft 0.4 Direct Sign-In with Bearer tokens and the Session header, run against each other as a CI pairing that passes with no expected failures.

**Architecture:** A new `net.openid.conformance.poppy` family, built the way OIDCC is built: the Company test extends `AbstractRedirectServerTestModule` and composes existing `condition.client` classes plus new `poppy.condition` classes; the Agent test extends `AbstractTestModule` and serves discovery, authorization, token and one API endpoint from `handleHttp`/`handleWellKnown`, composing existing `condition.as` and `condition.rs` classes plus new ones. `poppy.json` and the protected resource metadata must sit at the host root, so `TestDispatcher` gains one rule: a root `/.well-known/<file>` request is routed to the running test whose alias is `root-well-known`, and the CI nginx listens on 443 so the Company domain is a bare hostname as the spec requires. The pairing uses the existing `OUTER{INNER}inner-config outer-config` syntax of `run-test-plan.py`, with the Agent test as the waiting outer plan and the Company test as the nested driver.

**Tech Stack:** Java 21, Spring Boot, Gson via `OIDFJSON`, Nimbus JOSE+JWT, JUnit 5 + Mockito for condition unit tests, Python `run-test-plan.py` for the CI pairing.

**Spec:** `/Users/joseph/Documents/openid/poppy/pap-vs-fapi2-analysis.md` (section 6, revised for 0.4 on 2026-11-05) and the 0.4 spec text at `/Users/joseph/Documents/openid/poppy/spec-snapshot-2026-11-05/docs_0.4_spec.md`. Section numbers below (§) are that spec's.

## Global Constraints

- Protocol version string is `0.4`; header name `Poppy-Version`, header value `0.4` (§3.4). `Poppy-Extensions` is never sent by the suite in this plan.
- Direct Sign-In only (§4.2): `response_type=code`, PKCE `S256`, `scope` present, `state` present, `iss` in the response (RFC 9207).
- Client authentication is `private_key_jwt` signed with `ES256`; `aud` is the issuer as a single string (§4.1).
- Access tokens are `Bearer`. No DPoP in this plan (§4.5 makes DPoP the Company's choice; the suite as Company does not advertise `dpop_signing_alg_values_supported`).
- Session JWT (§5.2): header `typ` `poppy-session+jwt`, `alg` `ES256`, `kid` = RFC 7638 thumbprint of the delegated key, `poppy_dlg` = compact delegation; claims `iss` (client_id), `aud` (issuer), `sid`, `sub`, `htm`, `htu` (no query/fragment), `iat`, `jti`, `ath` (base64url SHA-256 of the access token) only when a token is sent.
- Delegation (§5.2): header `typ` `poppy-delegation+jwt`, `alg` `ES256`, `kid` = kid of the `jwks_uri` key; claims `iss`, `cnf.jwk` (public delegated key, no private members), `iat`, `exp` ≤ `iat` + 86400, `jti`. The suite as Agent uses `exp` = `iat` + 3600.
- Company identifier is the issuer. `organization.domain` is a hostname with no port (§3.1 "the host that served the document"; §3.2 `protected_resources` entries have "no path, port, or trailing slash"); it is compared, lowercased, with the host of the final `poppy.json` URL, and that URL must use the default HTTPS port. `https://{domain}` is the exact string the protected resource metadata `resource` and one `protected_resources` entry must equal. Nothing is relaxed for the suite's own emulator: in CI the nginx container gains a `listen 443 ssl;` line so both sides use host `nginx` with no port (Task 8), and a local run needs a 443 listener too or the domain checks fail as they should.
- Severities: a spec MUST on the side under test is `ConditionResult.FAILURE`; a SHOULD or descriptive text is `WARNING`; everything the suite's own emulator does is not validated (AGENTS.md "Emulated side vs side under test").
- Requirement tags use prefix `POPPY-` plus section number (`POPPY-5.2`). The link lands on the spec page top because the page uses slug anchors; accepted for this plan.
- Every new condition has a `*_UnitTest`. No module unit tests; the CI pairing covers module control flow (AGENTS.md "Test Naming Convention").
- Before every commit: `mvn -q compile` plus the task's targeted `mvn test -Dtest=...` with checkstyle and PMD enabled (no `-Dpmd.skip`, no `-Dcheckstyle.skip`); the full `mvn test` runs in Task 7 Step 3 and Task 9. The frontend gate `npm run test:ci` runs whenever a file under `src/main/resources/static/` changes.
- `OIDFJSON.getString(...)` and friends, never `JsonElement.getAsString()` (ArchUnit).
- Commit per task with the trailer `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`. Never push; never run `git stash`; work only in the worktree created in Task 0.
- Subagents and cost: every per-task implementer and every per-task reviewer that `subagent-driven-development` dispatches runs with `model: "sonnet"`; a task brief that is purely mechanical (Task 8's config and CI wiring) also passes `effort: "lower"`. Only the whole-branch review in Task 9 runs on the session's default model. Do not use `haiku`: it has previously reported green tests that were not run. The controller re-runs every verification command a subagent reports before accepting the task, and runs the final `mvn test` itself.

## Review Focus

Inputs the spec implies that no task's tests exercise, most likely to bite first:

1. A Company whose `poppy.json` redirects (`www.` to apex). `FetchPoppyJson` follows hops and checks every hop is HTTPS and the final path is exactly `/.well-known/poppy.json` with no query (§3). Test added in Task 2.
2. A Company whose `authorization_servers` lists two issuers and whose `poppy.json` has no `auth.issuer`. The Company test must fail with a message naming `auth.issuer` rather than pick one (§3.2). Test added in Task 2.
3. A token response with `scope` omitted. Omitted means "exactly as requested" (RFC 6749 §5.1), so the width check passes. Test added in Task 3.
4. An Agent that sends `Poppy-Session` on the `poppy.json` or token request. The Agent test ignores it on discovery and OAuth endpoints (§5.2 says those don't carry it, but no MUST NOT is on the Company), so no failure; the API endpoint is the only place it is required. No test needed; documented in Task 7.
5. A Session JWT whose `htu` carries the query string. RFC 9449 §4.3 comparison strips query and fragment from the request URL before comparing. Test added in Task 6.

---

### Task 0: Branch, spec library entry and family constants

**Files:**
- Modify: `src/main/java/net/openid/conformance/plan/TestPlan.java` (interfaces `ProfileNames`, `SpecFamilyNames`)
- Modify: `src/main/java/net/openid/conformance/statistics/SpecFamilyResolver.java:84-96` (`DECLARED_FAMILIES`)
- Modify: `src/main/java/net/openid/conformance/export/LogEntryHelper.java` (`specLinks`, near line 141)
- Modify: `scripts/spec_library.py` (`classify`, `OPENID_SOURCE_FORMATS`, `render_version`), `scripts/test_spec_library.py`, `library/specs/manifest.json`, `library/README.md` ("Adding a specification": one sentence on the new `plain-md` format); create `library/specs/openid/personal-agent-protocol-0_4.txt`
- Test: `python3 -m pytest scripts/test_spec_library.py` (or however that file is run today: read its header), existing `LogEntryHelper_UnitTest`, `SpecFamilyResolver_UnitTest` (if present), `mvn -q compile`

**Interfaces:**
- Produces: `TestPlan.ProfileNames.poppyCompanyTest = "Test a Poppy Company"`, `TestPlan.ProfileNames.poppyAgentTest = "Test a Poppy Personal Agent"`, `TestPlan.SpecFamilyNames.poppy = "Poppy"`, requirement prefix `POPPY-`, and the `@PublishTestModule(profile = "Poppy-0.4")` string that `run-test-plan.py` will match on in Task 8.

- [ ] **Step 1: Create the worktree and branch**

```bash
cd /Users/joseph/Documents/openid/conformance-suite
git fetch origin master
git worktree add -b poppy-happy-flow ../poppy-happy-flow-wt origin/master
```
The plan file is untracked in the main checkout, so copy it into the worktree (`mkdir -p ../poppy-happy-flow-wt/docs/superpowers/plans && cp docs/superpowers/plans/2026-11-05-poppy-happy-flow-pairing.md ../poppy-happy-flow-wt/docs/superpowers/plans/`) and commit it there as the first commit. All later steps run inside `../poppy-happy-flow-wt`.

- [ ] **Step 2: Add the constants**

In `TestPlan.ProfileNames` add `poppyCompanyTest` and `poppyAgentTest`; in `TestPlan.SpecFamilyNames` add `poppy`. In `SpecFamilyResolver.DECLARED_FAMILIES` add `SpecFamilyNames.poppy` in alphabetical position (after `oidccSessionManagement`, before whatever follows). Check whether `SpecFamilyResolver` reads a properties file of plan aliases (`legacy-plan-families`); a new family declared in code needs no entry there.

- [ ] **Step 3: Register the spec link and library text**

`classify()` in `scripts/spec_library.py:66-86` treats `personalagentprotocol.org` as a third-party host and would write an `excluded` entry; the `md` format (`:215-232`) renders mmark through xml2rfc and cannot take a plain Markdown page. AGENTS.md requires the manifest and the text to land with the Java change, so first extend the script:

- `classify()`: host `personalagentprotocol.org` with a `/docs/<version>/spec` link returns `{"kind": "plain-md"}`; `seed_skeleton` turns it into a document entry with `doc_id` `personal-agent-protocol-<version with _>` (`personal-agent-protocol-0_4`), `file` `openid/personal-agent-protocol-0_4.txt`, `source_url` = link URL + `.md`, `source_format` `plain-md`, `role` `linked`.
- `render_version()`: for `plain-md`, write the fetched Markdown as-is (UTF-8, LF) as the text; `text_headings()` must find its `## 3.4 Versions` style headings, so extend it to accept `#`-prefixed numbered headings, and `diff_headings()` compares them with the live page's `h2`/`h3` text.
- Tests in `scripts/test_spec_library.py`: `classify` returns `plain-md` for the Poppy link; `seed_skeleton` produces the entry above; `render_version` with a fake `fetch` returning two headings writes them unchanged and `text_headings` lists both.

Then in `LogEntryHelper.specLinks` add `specLinks.put("POPPY-", "https://personalagentprotocol.org/docs/0.4/spec#");` and run:

```bash
python3 scripts/spec_library.py seed
python3 scripts/spec_library.py sync --only personal-agent-protocol-0_4
python3 scripts/spec_library.py check
```
Compare the committed text with `/Users/joseph/Documents/openid/poppy/spec-snapshot-2026-11-05/docs_0.4_spec.md`; if the live page changed since 2026-11-05, re-read the changed sections before relying on any citation in this plan.

- [ ] **Step 4: Run the gate tests**

Run: `python3 -m pytest scripts/test_spec_library.py -q` and `mvn test -Dtest='LogEntryHelper_UnitTest,SpecFamilyResolver*'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/openid/conformance/plan/TestPlan.java src/main/java/net/openid/conformance/statistics/SpecFamilyResolver.java src/main/java/net/openid/conformance/export/LogEntryHelper.java library/ scripts/spec_library.py scripts/test_spec_library.py
git commit -m "Add the Poppy spec family and the POPPY- requirement prefix"
```

---

### Task 1: Route root well-known requests to the `root-well-known` alias

**Files:**
- Modify: `src/main/java/net/openid/conformance/runner/TestDispatcher.java:62-112`
- Test: `src/test/java/net/openid/conformance/runner/TestDispatcher_UnitTest.java`

**Interfaces:**
- Produces: `public static final String ROOT_WELL_KNOWN_ALIAS = "root-well-known"` on `TestDispatcher`. A request to `/.well-known/<file>` whose path has exactly one segment after `/.well-known/` is dispatched to `test.handleWellKnown("/.well-known/<file>", ...)` on the running test registered under that alias; 404 with `{"error": "No test found with alias 'root-well-known'"}` otherwise. Existing `/.well-known/<name>/test/a/<alias>/...` routing is unchanged.

- [ ] **Step 1: Write the failing test**

`TestDispatcher_UnitTest.java` exists but is an empty class, so there is no mocking style to follow. Extract the routing decision into a package-private static method on `TestDispatcher`, `static RouteTarget resolveRoute(String path, String finalPath, TestRunnerSupport support)` returning a record `RouteTarget(String testId, String restOfPath, ResponseEntity<?> error)` (`error` non-null means answer it and stop), and unit-test that with a Mockito `TestRunnerSupport`: `/.well-known/poppy.json` with `hasAlias("root-well-known")` true resolves to that alias's test id with `restOfPath` `poppy.json`; the same with `hasAlias` false yields a 404 whose body is `{"error": "No test found with alias 'root-well-known'"}`; `/.well-known/oauth-authorization-server/test/a/x` with `finalPath` `oauth-authorization-server/test/a/x` still resolves alias `x` with `restOfPath` empty; `/test/a/x/callback` resolves alias `x` with `restOfPath` `callback`; `/test/<id>/token` resolves `<id>`. `handle(...)` then calls `resolveRoute` and keeps everything else as it is.

- [ ] **Step 2: Run it to make sure it fails**

Run: `mvn test -Dtest=TestDispatcher_UnitTest`
Expected: FAIL (compile error: `resolveRoute` does not exist).

- [ ] **Step 3: Implement**

`resolveRoute`: if `path.startsWith(WELL_KNOWN)` and `finalPath` contains no `/`, the alias is `ROOT_WELL_KNOWN_ALIAS` and `restOfPath` is `finalPath`; otherwise the existing segment parsing (`a/<alias>` or `<id>`, with the extra two segments skipped for the well-known path-insertion form). Replace the inline parsing in `handle(...)` with the call. Keep the `CREATED` status guard and everything after it as it is.

- [ ] **Step 4: Run the tests**

Run: `mvn test -Dtest='TestDispatcher_UnitTest,PathMatchingTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/openid/conformance/runner/TestDispatcher.java src/test/java/net/openid/conformance/runner/TestDispatcher_UnitTest.java
git commit -m "Dispatch root .well-known requests to the test aliased root-well-known"
```

---

### Task 2: Company-side discovery conditions (suite as Agent)

**Files:**
- Create under `src/main/java/net/openid/conformance/poppy/condition/`:
  `ExtractPoppyCompanyDomainFromConfig`, `FetchPoppyJson`, `ValidatePoppyJsonCoreFields`, `CheckForUnexpectedFieldsInPoppyJson`, `EnsurePoppyAuthNotDisabled`, `FetchPoppyProtectedResourceMetadata`, `CheckForUnexpectedFieldsInPoppyProtectedResourceMetadata`, `SelectPoppyIssuerFromProtectedResourceMetadata`, `FetchPoppyAuthorizationServerMetadata`, `ValidatePoppyAuthorizationServerMetadata`
- Every `Fetch*` condition requires HTTP 200 and a JSON `Content-Type` (FAILURE, AGENTS.md "HTTP Endpoint Validation Checklist") and its unit test covers a 500 and a `text/html` response with a stubbed `RestTemplate`.
- Test: one `*_UnitTest` per class under `src/test/java/net/openid/conformance/poppy/condition/`, built like `CheckForAccessTokenValue_UnitTest` (`@Spy Environment`, `BsonEncoding.testInstanceEventLog()`, `cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO)`). Network-calling conditions are tested through a stubbed HTTP layer (look for `HoverflyExtension` or `MockRestServiceServer` use in existing `*_UnitTest`s of fetch conditions and copy that) so status, content type, redirect hops and body parsing are all exercised.

**Interfaces:**
- Consumes: `config` object (`poppy.company_domain` string, either a bare `host[:port]` or an `https://` URL).
- Produces env entries used by Task 4:
  - `poppy_domain` (string, lowercased hostname with no port, e.g. `nginx`; first set from config, then overwritten with the final `poppy.json` URL host once validated)
  - `poppy_json` (object), `poppy_json_final_url` (string)
  - `poppy_protected_resource_metadata` (object)
  - `poppy_issuer` (string)
  - `server` (object, RFC 8414 metadata; same key the existing client conditions read)

- [ ] **Step 1: `ExtractPoppyCompanyDomainFromConfig`**

Test: `"https://Example.com/"` and `"Example.com"` both yield `poppy_domain` = `example.com`; `"example.com:8443"` and `"https://example.com:8443"` throw with a message saying Poppy requires the Company's well-known documents on the default HTTPS port; `"http://example.com"` throws; missing field throws with message containing `'Company domain' field is missing from the 'Poppy' section in the test configuration`.
Implement: `@PreEnvironment(required = "config") @PostEnvironment(strings = "poppy_domain")`.

- [ ] **Step 2: `FetchPoppyJson`**

Test the hop checker as a package-private static `checkHops(List<URI> hops)` (throws on any non-HTTPS hop; throws when the last URI's path is not exactly `/.well-known/poppy.json` or has a query; throws when the last URI has an explicit port; passes for a 2-hop `www.example.com` to `example.com` redirect). Test that the start URL for `poppy_domain` `example.com` is `https://example.com/.well-known/poppy.json`, and with the stubbed HTTP layer that a `www.` to apex redirect ends with `poppy_json_final_url` `https://example.com/.well-known/poppy.json`.
Implement: GET with redirects followed manually (max 5 hops; use a `RestTemplate` whose request factory has redirects disabled, as other suite conditions do for `request_uri` fetches), parse JSON, `@PostEnvironment(required = "poppy_json", strings = "poppy_json_final_url")`. Requirement `POPPY-3`.

- [ ] **Step 3: `ValidatePoppyJsonCoreFields`**

Test: passes for the §3 example document with `protocol_version` changed to `0.4` and `organization.domain` `example.com` against `poppy_json_final_url` `https://example.com/.well-known/poppy.json`, and afterwards `poppy_domain` is `example.com` even when it was `www.example.com` before (the document speaks for the host that served it, §3); fails for `protocol_version` `0.3` (message names the version), for `organization.domain` `other.example`, for `organization.domain` `example.com:443` (port not allowed), for a document with none of `agent`/`apis`/`web`, and for an `http://` value in any URL-valued field (`web.browser_session_endpoint`, `apis[].url`, `agent.protocols[].endpoint`, `feedback.endpoint`, `excluded_tasks.url`, `organization.logo_uri`, `auth.issuer`).
Implement: `@PreEnvironment(required = "poppy_json", strings = "poppy_json_final_url") @PostEnvironment(strings = "poppy_domain")`; the comparison uses the final URL's host, lowercased. Requirements `POPPY-3`, `POPPY-3.1`.

- [ ] **Step 3a: `CheckForUnexpectedFieldsInPoppyJson`**

Test: the §3 example passes; an unknown top-level key `foo` or unknown `organization.colour` throws (caller runs it at WARNING: receivers MUST ignore unknown fields, §3.3, but the suite tests senders, AGENTS.md "Sender vs Receiver Validation"). Known keys are the §3.1 field table of the 0.4 text (`docs_0.4_spec.md:135-156`): top level `protocol_version`, `supported_versions`, `organization`, `auth`, `agent`, `apis`, `web`, `feedback`, `excluded_tasks`, `skill_index`, `extensions`; `organization` keys `name`, `domain`, `logo_uri`; `auth` keys `issuer`, `disabled`, `scopes`, `scope_hints`; `web` keys `browser_session_endpoint`, `disabled`, `unscoped`; `agent` key `protocols` with entries `type`, `endpoint`, `access`; `apis` entries `type`, `url`, `description`; `feedback` key `endpoint`; `excluded_tasks` key `url`. Add a test that a top-level `skills` (a plausible misspelling) warns and `skill_index` passes. Read §3.1 once more before writing the lists.

- [ ] **Step 3c: `CheckForUnexpectedFieldsInPoppyProtectedResourceMetadata`**

Test: `{"resource", "authorization_servers", "jwks_uri", "scopes_supported", "bearer_methods_supported", "resource_signing_alg_values_supported", "resource_name", "resource_documentation", "resource_policy_uri", "resource_tos_uri", "tls_client_certificate_bound_access_tokens", "authorization_details_types_supported", "dpop_signing_alg_values_supported", "dpop_bound_access_tokens_required", "signed_metadata"}` (RFC 9728 §2) pass; `foo` throws (caller WARNING). For the AS metadata reuse the existing `CheckForUnexpectedParametersInServerMetadata` (`@PreEnvironment(required = "server")`) at WARNING; check that its allow-list includes `protected_resources` (RFC 9728 §4) and add it if not.

- [ ] **Step 3b: `EnsurePoppyAuthNotDisabled`**

Test: `auth.disabled` absent or `false` passes; `true` fails with a message that this module tests Direct Sign-In, which §3.2 forbids an Agent to start while `auth.disabled` is set, and that the Company must enable sign-in for Personal Agents before running it. Requirement `POPPY-3.2`. Caller: `callAndStopOnFailure`.

- [ ] **Step 4: `FetchPoppyProtectedResourceMetadata`**

Test: derived URL for `poppy_domain` `example.com` is `https://example.com/.well-known/oauth-protected-resource`; a 404 throws with a message saying the Company publishes no issuer and this test needs sign-in (not a skip: the module tests Direct Sign-In).
Implement: `@PreEnvironment(strings = "poppy_domain") @PostEnvironment(required = "poppy_protected_resource_metadata")`. Requirement `POPPY-3.2`, `RFC9728-3`.

- [ ] **Step 5: `SelectPoppyIssuerFromProtectedResourceMetadata`**

Test: `resource` must equal `https://` + `poppy_domain` exactly (fail on trailing slash); every `authorization_servers` entry, `resource`, and any `jwks_uri` or `*_endpoint` key present must be an `https://` URL (fail on `http://`); one entry becomes `poppy_issuer`; two entries with no `auth.issuer` in `poppy_json` fails with a message containing `auth.issuer`; two entries with `auth.issuer` naming one of them selects it; `auth.issuer` present but not in the list fails; empty list fails ("Company offers no sign-in").
Implement: `@PreEnvironment(required = {"poppy_protected_resource_metadata", "poppy_json"}, strings = "poppy_domain") @PostEnvironment(strings = "poppy_issuer")`. Requirement `POPPY-3.2`.

- [ ] **Step 6: `FetchPoppyAuthorizationServerMetadata`**

Test: the URL for issuer `https://as.example.com/test/a/x` is `OAuthUriUtil.generateWellKnownUrlForPath(issuer, "oauth-authorization-server")` (RFC 8414 §3.1 path insertion).
Implement: `@PreEnvironment(strings = "poppy_issuer") @PostEnvironment(required = "server")`; GET, require 200 and JSON. Requirement `RFC8414-3.1`.

- [ ] **Step 7: `ValidatePoppyAuthorizationServerMetadata`**

Test: fails when `issuer` differs from `poppy_issuer` by a trailing slash; fails when `protected_resources` lacks `https://` + `poppy_domain`; fails when `grant_types_supported` lacks `refresh_token` or `authorization_code` (message says the Company does not offer Direct Sign-In); fails when `token_endpoint`, `authorization_endpoint` or `revocation_endpoint` is missing; fails when any URL-valued field present is not an `https://` URL: `issuer`, `jwks_uri`, every key ending `_endpoint`, every `protected_resources` entry (§3 "Every URL this protocol defines or exchanges MUST be an HTTPS URL", tested with an `http://` `device_authorization_endpoint` and an `http://` `protected_resources` entry); passes on the §3.2 example with `protected_resources` containing the domain.
Implement: `@PreEnvironment(required = "server", strings = {"poppy_issuer", "poppy_domain"})`. Requirement `POPPY-3.2`.

- [ ] **Step 8: Run the tests**

Run: `mvn test -Dtest='net.openid.conformance.poppy.condition.*_UnitTest'` (checkstyle and PMD on)
Expected: PASS, 10 test classes.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/net/openid/conformance/poppy/condition src/test/java/net/openid/conformance/poppy/condition
git commit -m "Poppy: Company discovery conditions for the suite acting as a Personal Agent"
```

---

### Task 3: Company-side identity, Session and token-response conditions (suite as Agent)

**Files:**
- Create under `src/main/java/net/openid/conformance/poppy/condition/`:
  `CreatePoppyClientMetadataDocument`, `GeneratePoppyDelegatedKey`, `CreatePoppyDelegation`, `CreatePoppySession`, `CreatePoppySessionJwt`, `AddPoppyHeadersToResourceRequest`, `EnsurePoppyTokenTypeIsBearer`, `EnsurePoppyGrantedScopeNotWiderThanRequested`, `EnsurePoppyResourceResponseStatusIs2xx`
- Create: `src/main/java/net/openid/conformance/poppy/PoppyConstants.java` with `PROTOCOL_VERSION = "0.4"`, `SESSION_JWT_TYP = "poppy-session+jwt"`, `DELEGATION_JWT_TYP = "poppy-delegation+jwt"`, `HEADER_SESSION = "Poppy-Session"`, `HEADER_VERSION = "Poppy-Version"`, `DELEGATION_LIFETIME_SECONDS = 3600`, `DELEGATION_MAX_LIFETIME_SECONDS = 86400`.
- Test: one `*_UnitTest` per condition.

**Interfaces:**
- Consumes: `client` (from `GetStaticClientConfiguration`: `client.jwks`, `client.scope`), `client_jwks` and `client_public_jwks` (from `ExtractJWKsFromStaticClientConfiguration`), `poppy_issuer`, `access_token` (`value`, `type`; from `ExtractAccessTokenFromTokenResponse`), `token_endpoint_response`, `resource_endpoint_response_full`, `protected_resource_url`.
- Produces: `client.client_id`, `client.redirect_uris`, `client.jwks_uri`, `client_id` (string), `redirect_uri` (string), `poppy_client_metadata_document` (object, what `agent.json` serves), `poppy_delegated_key` (object, private EC JWK with `kid`), `poppy_delegation` (string), `poppy_sid` (string), `poppy_user_id` (string), `poppy_session_jwt` (string), `resource_endpoint_request_headers` (object, read by `CallProtectedResource`).

- [ ] **Step 1: `CreatePoppyClientMetadataDocument`**

Test: with env string `base_url` `https://h:8443/test/a/al` (no trailing slash: `TestRunner.java:406` builds it as `baseUrl + "/test/" + path`; the module stores the `baseUrl` argument of `configure(...)` under this key as `AbstractFAPI2SPFinalServerTestModule` does) produces `client_id` `https://h:8443/test/a/al/agent.json`, `jwks_uri` `https://h:8443/test/a/al/jwks.json`, `redirect_uris` `["https://h:8443/test/a/al/callback"]`; with a trailing slash on `base_url` the same URLs result (normalise first), `token_endpoint_auth_method` `private_key_jwt`, `client_name` `OpenID Foundation conformance suite`; also writes `client_id` and `redirect_uri` strings and copies `client_id`, `redirect_uris`, `jwks_uri` into `client`.
Implement: `@PreEnvironment(required = "client", strings = "base_url") @PostEnvironment(required = {"poppy_client_metadata_document", "client"}, strings = {"client_id", "redirect_uri"})`. Requirement `POPPY-4.1`.

- [ ] **Step 2: `GeneratePoppyDelegatedKey`**

Test: output is an EC P-256 JWK with `d`, `alg` `ES256`, `use` absent, and `kid` equal to its RFC 7638 thumbprint (compute with Nimbus `ECKey.computeThumbprint()`); two calls produce different keys.
Implement: `@PostEnvironment(required = "poppy_delegated_key")`. Look at `GenerateDpopKey` for the Nimbus generation idiom.

- [ ] **Step 3: `CreatePoppyDelegation`**

Test: parse the output with Nimbus; header `typ` = `poppy-delegation+jwt`, `alg` = `ES256`, `kid` = the signing key's `kid`; claims `iss` = `client_id`, `cnf.jwk` equals the public form of `poppy_delegated_key` (no `d`), `iat` within 5 s of now, `exp` = `iat` + 3600, `jti` ≥ 16 chars; signature verifies with the public key from `client_public_jwks`. Fails when `client_jwks` has no ES256 key.
Implement: `@PreEnvironment(required = {"client_jwks", "poppy_delegated_key"}, strings = "client_id") @PostEnvironment(strings = "poppy_delegation")`. Pick the first `client_jwks` key with `alg` `ES256` (or `kty` `EC` + `crv` `P-256`). Requirement `POPPY-5.2`.

- [ ] **Step 4: `CreatePoppySession`**

Test: `poppy_sid` is 22 base64url characters and differs between calls; `poppy_user_id` starts `usr_` and is at least 10 chars.
Implement: `@PostEnvironment(strings = {"poppy_sid", "poppy_user_id"})`. Requirement `POPPY-5.2`, `POPPY-5.1`.

- [ ] **Step 5: `CreatePoppySessionJwt`**

Test: with `poppy_resource_request_method` `GET`, `protected_resource_url` `https://api.example/x?y=1`, `access_token.value` `tok`: header `typ` `poppy-session+jwt`, `alg` `ES256`, `kid` = delegated key thumbprint, `poppy_dlg` = `poppy_delegation`; claims `iss` = `client_id`, `aud` = `poppy_issuer`, `sid`, `sub` = `poppy_user_id`, `htm` `GET`, `htu` `https://api.example/x` (query stripped), `iat` now, `jti` unique, `ath` = base64url(SHA-256("tok")) with no padding; signature verifies with `poppy_delegated_key`'s public key. Without `access_token` in env, `ath` is absent.
Implement: `@PreEnvironment(required = {"poppy_delegated_key"}, strings = {"client_id", "poppy_issuer", "poppy_sid", "poppy_user_id", "poppy_delegation", "protected_resource_url", "poppy_resource_request_method"}) @PostEnvironment(strings = "poppy_session_jwt")`. Requirement `POPPY-5.2`.

- [ ] **Step 6: `AddPoppyHeadersToResourceRequest`**

Test: `resource_endpoint_request_headers` gains `Poppy-Session` = `poppy_session_jwt` and `Poppy-Version` = `0.4`, keeping any headers already there.
Implement: `@PreEnvironment(strings = "poppy_session_jwt") @PostEnvironment(required = "resource_endpoint_request_headers")`. Requirement `POPPY-3.4`, `POPPY-5.2`.

- [ ] **Step 7: `EnsurePoppyTokenTypeIsBearer`**

Test: `token_endpoint_response.token_type` `Bearer` (any case) passes; `DPoP` fails with a message saying this module did not send a DPoP proof so a DPoP-bound token cannot be used; missing fails.
Implement: `@PreEnvironment(required = "token_endpoint_response")`. Requirement `RFC6749-5.1`.

- [ ] **Step 8: `EnsurePoppyGrantedScopeNotWiderThanRequested`**

Test: requested `client.scope` `orders:read`; response `scope` absent passes (RFC 6749 §5.1: identical); `orders:read` passes; `orders:read orders:write` fails naming `orders:write`; empty string fails (§4.7 "grants at least one scope").
Implement: `@PreEnvironment(required = {"client", "token_endpoint_response"})`. Requirement `POPPY-4.7`.

- [ ] **Step 9: `EnsurePoppyResourceResponseStatusIs2xx`**

Test: `resource_endpoint_response_full.status` 200 and 204 pass; 401 fails with the `www-authenticate` header value in `args`; 400 with body `{"error":"invalid_session"}` fails with the body in `args`.
Implement: `@PreEnvironment(required = "resource_endpoint_response_full")`. Requirement `POPPY-5.3`.

- [ ] **Step 10: Run the tests**

Run: `mvn test -Dtest='net.openid.conformance.poppy.condition.*_UnitTest'`
Expected: PASS, 19 test classes.

- [ ] **Step 11: Commit**

```bash
git add src/main/java/net/openid/conformance/poppy src/test/java/net/openid/conformance/poppy
git commit -m "Poppy: client metadata, delegation and Session JWT conditions for the suite acting as a Personal Agent"
```

---

### Task 4: Company test module, plan and configuration fields

**Files:**
- Create: `src/main/java/net/openid/conformance/poppy/company/AbstractPoppyCompanyTestModule.java`, `PoppyCompanyHappyFlow.java`, `PoppyCompanyTestPlan.java`
- Modify: `src/main/resources/static/js/config-field-catalog.json` (new section `poppy`, field `poppy.company_domain`)
- Test: `mvn test -Dtest='*ArchUnit*'`, `npm run test:ci` in `frontend/` (unconditional: the catalog is a static asset the frontend tests read; Node 24 via the nix shell, see `frontend/README.md`), then a manual run against the Agent test once Task 7 exists (Task 8 automates it).

**Interfaces:**
- Consumes: everything Task 2 and Task 3 produce; existing conditions named below.
- Produces: test module `poppy-company-happy-flow`, plan `poppy-company-test-plan`, and the HTTP paths `agent.json`, `jwks.json`, `callback` under the module's base URL.

- [ ] **Step 1: `AbstractPoppyCompanyTestModule extends AbstractRedirectServerTestModule`**

`@VariantParameters({})` is not needed: no variants in this plan. `@ConfigurationFields({"poppy.company_domain", "client.jwks", "client.scope", "resource.resourceUrl"})`. Configuration-error messages name the UI labels (`'Company domain' field in the 'Poppy' section`, AGENTS.md "Error Messages for Configuration Issues").

`configure(JsonObject config, String baseUrl, String externalUrlOverride)`: `env.putObject("config", config)`; `env.putString("base_url", baseUrl)`; copy `config.client` into env object `client` and `config.resource` into env object `resource` (do not use `GetStaticClientConfiguration`, which insists on a configured `client_id` that Poppy derives instead); `CreatePoppyClientMetadataDocument` (sets `client.client_id`); `ExtractJWKsFromStaticClientConfiguration`; `exposeEnvString("client_id")`; `setStatus(Status.CONFIGURED)`; `fireSetupDone()`.

`handleHttp`: `agent.json` returns `poppy_client_metadata_document` as `application/json`; `jwks.json` returns `client_public_jwks`; otherwise `super.handleHttp` (the base handles `callback` and the implicit submit path).

`start()`: block "Discovery": `ExtractPoppyCompanyDomainFromConfig`, `FetchPoppyJson`, `ValidatePoppyJsonCoreFields`, `CheckForUnexpectedFieldsInPoppyJson` at `WARNING`, `EnsurePoppyAuthNotDisabled`, `FetchPoppyProtectedResourceMetadata`, `CheckForUnexpectedFieldsInPoppyProtectedResourceMetadata` at `WARNING`, `SelectPoppyIssuerFromProtectedResourceMetadata`, `FetchPoppyAuthorizationServerMetadata`, `ValidatePoppyAuthorizationServerMetadata`, `CheckForUnexpectedParametersInServerMetadata` at `WARNING`, then the common `CheckServerConfiguration` only if its checks apply to plain OAuth metadata (read it; if it demands OIDC fields, leave it out). Block "Authorization request": `CreateRandomStateValue`, `CreateAuthorizationEndpointRequestFromClientInformation` (sets `client_id`, `redirect_uri`, `scope` from `client.scope`), `AddStateToAuthorizationEndpointRequest`, `SetAuthorizationEndpointRequestResponseTypeToCode` (find the existing condition name with `ls condition/client | grep ResponseType`), `sequence(SetupPkceAndAddToAuthorizationRequest.class)`, `BuildPlainRedirectToAuthorizationEndpoint`, then `performRedirect()`.

`processCallback()`: `setStatus(RUNNING)`; `env.mapKey("authorization_endpoint_response", "callback_query_params")`; `CheckIfAuthorizationEndpointError`; `CheckStateInAuthorizationResponse`; `RequireIssInAuthorizationResponse` with `.requirement("RFC9207-2")` at FAILURE; `ExtractAuthorizationCodeFromAuthorizationResponse`; block "Token endpoint": `CreateTokenEndpointRequestForAuthorizationCodeGrant`, `AddCodeVerifierToTokenEndpointRequest`, `sequence(CreateJWTClientAuthenticationAssertionWithIssAudAndAddToTokenEndpointRequest.class)` (its claims condition reads `server.issuer` for `aud`: confirm by reading `CreateClientAuthenticationAssertionClaimsWithIssAudience`), `CallTokenEndpointAndReturnFullResponse`, `CheckIfTokenEndpointResponseError`, `CheckTokenEndpointHttpStatus200`, `CheckTokenEndpointReturnedJsonContentType`, `CheckForAccessTokenValue`, `EnsurePoppyTokenTypeIsBearer`, `CheckForRefreshTokenValue` at `WARNING` with `.requirement("POPPY-4.7")` (WARNING, not FAILURE: §4.4, §4.7 and §4.8 state the refresh token descriptively with no BCP 14 keyword, analysis gap 12), `ExtractExpiresInFromTokenEndpointResponse` at `WARNING` then `skipIfMissing(new String[]{"expires_in"}, null, ConditionResult.INFO, ValidateExpiresIn.class, ConditionResult.WARNING)` as `AbstractFAPI2SPFinalServerTestModule.java:918` does, `ExtractGrantedScopeFromTokenEndpointResponse`, `EnsurePoppyGrantedScopeNotWiderThanRequested`, `ExtractAccessTokenFromTokenResponse`. Block "Protected resource with Session header": `SetProtectedResourceUrlToSingleResourceEndpoint` (reads env object `resource.resourceUrl`, which `configure` copied), `env.putString("poppy_resource_request_method", "GET")`, `GeneratePoppyDelegatedKey`, `CreatePoppyDelegation`, `CreatePoppySession`, `CreatePoppySessionJwt`, `AddPoppyHeadersToResourceRequest`, `CallProtectedResource`, `EnsurePoppyResourceResponseStatusIs2xx`, `EnsureResourceResponseReturnedJsonContentType` at `WARNING`. Then `fireTestFinished()`.

- [ ] **Step 2: `PoppyCompanyHappyFlow`**

```java
@PublishTestModule(
  testName = "poppy-company-happy-flow",
  displayName = "Poppy Company: happy flow (Direct Sign-In, Bearer token, Session header)",
  summary = "Fetches poppy.json, the protected resource metadata and the OAuth server metadata for the configured Company domain, signs the user in with Direct Sign-In (authorization code + PKCE S256, private_key_jwt with an ES256 key), then calls the configured protected resource once with a Bearer access token and a Poppy-Session header signed with a delegated key. The Personal Agent's client metadata document is served by the suite at the client_id URL shown in the test log; the Company must accept that client_id.",
  profile = "Poppy-0.4")
```
No body beyond the class declaration unless `start()` needs a module-specific hook.

- [ ] **Step 3: `PoppyCompanyTestPlan`**

`@PublishTestPlan(testPlanName = "poppy-company-test-plan", displayName = "Poppy 0.4 Company test plan (alpha, not part of a certification program)", profile = TestPlan.ProfileNames.poppyCompanyTest, specFamily = TestPlan.SpecFamilyNames.poppy, testModules = {PoppyCompanyHappyFlow.class})`. Check whether `@PublishTestPlan` requires a `summary` or certification profile method; copy `AuthzenPDPEvaluationsTestPlan` for the shape and omit certification profile naming (the plan is not certifiable; see `cert-profile-display-name-taxonomy` for the suffix convention).

- [ ] **Step 4: Config field catalog**

Add a section `{"key": "poppy", "title": "Poppy", "fields": [{"key": "poppy.company_domain", "label": "Company domain", "type": "string", "tooltip": "Company test: the Company's domain (a hostname, no port; an https:// URL is accepted). The suite fetches https://<domain>/.well-known/poppy.json and https://<domain>/.well-known/oauth-protected-resource. Personal Agent test: the hostname at which this conformance suite is reachable on port 443, announced as organization.domain."}]}` following the shape of the `server` section. Run `npm run test:ci` in `frontend/`.

- [ ] **Step 5: Compile and check the plan is published**

Run: `mvn -B -Dmaven.test.skip -Dpmd.skip clean package` then start the dev profile and `curl -sk https://localhost.emobix.co.uk:8443/api/plan/available | python3 -c 'import json,sys; print([p for p in json.load(sys.stdin) if "poppy" in p["planName"]])'` (adjust to the real endpoint listed in `/v3/api-docs`).
Expected: the plan appears with one module.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/net/openid/conformance/poppy/company src/main/resources/static/js/config-field-catalog.json
git commit -m "Poppy: Company test plan with a Direct Sign-In happy flow"
```

---

### Task 5: Agent-side document and client-metadata conditions (suite as Company)

**Files:**
- Create under `src/main/java/net/openid/conformance/poppy/condition/`:
  `EnsurePoppyRootWellKnownAlias`, `GeneratePoppyCompanyDocuments`, `FetchPoppyClientMetadataDocument`, `ValidatePoppyClientMetadataDocument`, `CheckForUnexpectedFieldsInPoppyClientMetadataDocument`, `FetchPoppyClientJwks`, `EnsurePoppyClientJwksContainsOnlyPublicKeys` (subclass of `condition.as.AbstractEnsureJwksDoesNotContainPrivateOrSymmetricKeys` reading `client.jwks`; §4.1 "The key set contains only public keys"; test: a JWK with `d` fails, an `oct` key fails, the public ES256 key passes)
- Test: one `*_UnitTest` per class.

**Interfaces:**
- Consumes: `config` (`alias`, `poppy.company_domain`), `base_url` (module base URL `https://h:8443/test/a/root-well-known`, no trailing slash), `incoming_request` (for the `client_id` the Agent sends).
- Produces: `poppy_json`, `poppy_protected_resource_metadata`, `server` (objects, served as-is), `issuer` (string; `AddIssToAuthorizationEndpointResponseParams` reads it), `poppy_domain`, `client` (object: the fetched metadata document, later extended with `jwks` by `FetchClientKeys`).

- [ ] **Step 1: `EnsurePoppyRootWellKnownAlias`**

Test: `config.alias` `root-well-known` passes; `other` fails with a message containing `alias` and `root-well-known`; missing fails.
Implement: `@PreEnvironment(required = "config")`. Compare with `TestDispatcher.ROOT_WELL_KNOWN_ALIAS`.

- [ ] **Step 2: `GeneratePoppyCompanyDocuments`**

Test: for `base_url` `https://h:8443/test/a/root-well-known` and `config.poppy.company_domain` `h`: `issuer` = `https://h:8443/test/a/root-well-known` (the base URL as given, trailing slash stripped if present); `poppy_domain` = `h`; a configured domain with a port or scheme fails with a configuration message; `poppy_json` has `protocol_version` `0.4`, `organization.name` `OpenID Foundation conformance suite`, `organization.domain` `h`, `auth.scopes` `{"orders:read": "See your orders"}`, `auth.scope_hints` `{"orders:read": {"changes": "none", "personal_data": true}}`, `apis` `[{"type": "openapi", "url": "<issuer>/openapi.json", "description": "Conformance suite test API"}]`, no `web`, no `agent`; `poppy_protected_resource_metadata` = `{"resource": "https://h", "authorization_servers": ["<issuer>"]}`; `server` has `issuer`, `authorization_endpoint` `<issuer>/authorize`, `token_endpoint` `<issuer>/token`, `revocation_endpoint` `<issuer>/revoke`, `grant_types_supported` `["authorization_code", "refresh_token"]`, `scopes_supported` `["orders:read"]`, `protected_resources` `["https://h"]`, `token_endpoint_auth_methods_supported` `["private_key_jwt"]`, `token_endpoint_auth_signing_alg_values_supported` `["ES256", "PS384"]`, `code_challenge_methods_supported` `["S256"]`, `response_types_supported` `["code"]`.
Implement: `@PreEnvironment(required = "config", strings = "base_url") @PostEnvironment(required = {"poppy_json", "poppy_protected_resource_metadata", "server"}, strings = {"issuer", "poppy_domain"})`. The issuer keeps its port: §3.2 forbids a port only in `protected_resources` entries and `organization.domain`, not in the issuer URL.

- [ ] **Step 3: `FetchPoppyClientMetadataDocument`**

Test with the stubbed HTTP layer: a 302 response is rejected (message cites CIMD §5 and `POPPY-5.3`); a non-`https` `client_id` is rejected before any fetch; a 404, a 500 and a `text/html` 200 are rejected; a 200 `application/json` stores the body as `client`.
Implement: `@PreEnvironment(strings = "poppy_incoming_client_id") @PostEnvironment(required = "client")`; GET without following redirects; the `client_id` comes from the authorization request (Task 7 copies it to `poppy_incoming_client_id`). Requirement `POPPY-4.1`, `POPPY-5.3`.

- [ ] **Step 4: `ValidatePoppyClientMetadataDocument`**

Test: document from §4.1 with `client_id` equal to `poppy_incoming_client_id` passes; `client_id` differing fails; `jwks_uri` on another host fails; `jwks_uri` `http://` fails; a `redirect_uris` entry on another host fails; `http://127.0.0.1:49152/cb` passes (loopback, RFC 8252 §7.3); `http://localhost/cb` fails (not a loopback IP literal); `token_endpoint_auth_method` present and not `private_key_jwt` fails; absent passes.
Implement: `@PreEnvironment(required = "client", strings = "poppy_incoming_client_id")`. "Same domain" means same host as `client_id` (ports ignored). Requirement `POPPY-4.1`.

- [ ] **Step 4a: `CheckForUnexpectedFieldsInPoppyClientMetadataDocument`**

Test: the §4.1 example passes; `client_colour` throws (caller WARNING). Known keys: RFC 7591 §2 client metadata names plus `client_id` (CIMD). Requirement `POPPY-4.1`.

- [ ] **Step 4b: `FetchPoppyClientJwks`**

Test with the stubbed HTTP layer: GET `client.jwks_uri` without following redirects; 200 with `application/json` or `application/jwk-set+json` and a parseable key set stores it as `client.jwks`; 302, 404, 500, `text/html`, and a body that is not a JWK set each throw. Replaces `FetchClientKeys`, which validates neither status nor content type (`FetchClientKeys.java:41-47`). `@PreEnvironment(required = "client") @PostEnvironment(required = "client")`. Requirement `POPPY-4.1`.

- [ ] **Step 5: Run the tests and commit**

Run: `mvn test -Dtest='net.openid.conformance.poppy.condition.*_UnitTest'`
Expected: PASS, 25 classes.

```bash
git add src/main/java/net/openid/conformance/poppy/condition src/test/java/net/openid/conformance/poppy/condition
git commit -m "Poppy: Company documents and client metadata validation for the suite acting as a Company"
```

---

### Task 6: Agent-side authorization, token and Session verification conditions (suite as Company)

**Files:**
- Create under `src/main/java/net/openid/conformance/poppy/condition/`:
  `EnsurePoppyAuthorizationRequestContainsS256CodeChallenge`, `EnsurePoppyAuthorizationRequestContainsScope`, `EnsurePoppyRedirectUriMatchesClientMetadata`, `EnsurePoppyClientAssertionAlgIsAdvertised`, `EnsurePoppyClientAssertionContainsIat`, `EnsurePoppyClientAssertionIsShortLived`, `ExtractPoppySessionHeaders`, `EnsurePoppyVersionIsSupported`, `ValidatePoppyDelegation`, `ValidatePoppySessionJwt`, `EnsurePoppyNoCredentialsInQuery`
- Test: one `*_UnitTest` per class. For the JWT validators, build fixtures with Nimbus in the test: a `jwks_uri` ES256 key pair, a delegated key pair, a valid delegation and Session JWT, then mutate one thing per test.

**Interfaces:**
- Consumes: `effective_authorization_endpoint_request` (`CreateEffectiveAuthorizationRequestParameters.ENV_KEY`), `client_assertion` (from `ExtractClientAssertion`: object with `claims` and header; read that class for the exact shape), `incoming_request` (`headers`, `method`, `request_url`, `query_string_params`), `client` (with `jwks` after `FetchClientKeys`), `issuer`, `incoming_access_token` (from `rs.ExtractBearerAccessTokenFromHeader`), `access_token` (the token the suite issued), `poppy_incoming_client_id`.
- Produces: `code_challenge`, `code_challenge_method` (strings, read by `ValidateCodeVerifierWithS256` inside the `CheckPkceCodeVerifier` sequence), `authorization_endpoint_request_redirect_uri` (string, read by `ValidateRedirectUriForTokenEndpointRequest`), `poppy_session_jwt`, `poppy_version` (strings), `poppy_delegation_claims`, `poppy_session_claims` (objects), `poppy_delegated_public_jwk` (object).

- [ ] **Step 1: `EnsurePoppyAuthorizationRequestContainsS256CodeChallenge`**

Test: `code_challenge` 43 chars with `code_challenge_method` `S256` passes and exports both strings; missing challenge fails; method `plain` fails; method absent fails.
Requirement `POPPY-4.2`, `RFC7636-4.3`.

- [ ] **Step 2: `EnsurePoppyAuthorizationRequestContainsScope`**

Test: `scope` `orders:read` passes; missing or empty fails (§4.7 "Each sign-in request MUST include `scope`"); a scope the emulated Company does not define (`PoppyConstants.COMPANY_SCOPES = Set.of("orders:read")`, the set the emulator both publishes and enforces) fails with `invalid_scope` in the message. Do not test against `scopes_supported` as such: §4.7 lets an Agent request scopes documented by APIs or extensions that the list omits.
Requirement `POPPY-4.7`.

- [ ] **Step 2a: `EnsurePoppyRedirectUriMatchesClientMetadata`**

Test: request `redirect_uri` equal to an entry of `client.redirect_uris` passes and exports `authorization_endpoint_request_redirect_uri`; different path fails; `http://127.0.0.1:50000/cb` against a registered `http://127.0.0.1:49152/cb` passes (RFC 8252 §7.3 port exception, §4.2); `https://agent.example:9000/cb` against `https://agent.example/cb` fails (port exception is loopback-only); absent `redirect_uri` fails. Replaces `EnsureValidRedirectUriForAuthorizationEndpointRequest`, whose exact-string match (`:49`) has no loopback exception.
Requirement `POPPY-4.2`.

- [ ] **Step 3: `EnsurePoppyClientAssertionAlgIsAdvertised`**

Test: header `alg` in `server.token_endpoint_auth_signing_alg_values_supported` (`ES256`, `PS384`) passes; `RS256` fails; `none` fails. The failure is the Agent ignoring the algorithms the emulated Company advertised (RFC 8414 §2, RFC 7523 §3), not a Poppy MUST: §4.1 obliges the Company to accept ES256 and says nothing about what an Agent may use at a Company that accepts more.
Requirement `RFC8414-2`.

- [ ] **Step 4: `EnsurePoppyClientAssertionIsShortLived`**

Two conditions. `EnsurePoppyClientAssertionContainsIat`: missing `iat` fails (§4.1 MUST include `iat` and a unique `jti`; `jti` presence is already checked by `ValidateClientAssertionClaims`), caller FAILURE. `EnsurePoppyClientAssertionIsShortLived`: `exp - iat` 300 passes, 301 fails, caller WARNING (the 5-minute figure is guides-only; the spec says "short-lived").
Requirement `POPPY-4.1`.

- [ ] **Step 5: `ExtractPoppySessionHeaders`**

Test: lowercased `incoming_request.headers` `poppy-session` and `poppy-version` are copied to `poppy_session_jwt` and `poppy_version`; missing `poppy-session` fails with a message that the Company returns `invalid_session` (§5.3 MUST reject when handling depends on Session claims); missing `poppy-version` fails with a message naming `version_unsupported` (§3.4 MUST check).
Requirement `POPPY-5.2`, `POPPY-3.4`.

- [ ] **Step 6: `EnsurePoppyVersionIsSupported`**

Test: `0.4` passes; `0.3`, `1.0`, `0.4.1`, `v0.4` fail (pre-1.0 compatibility is an identical `major.minor`).
Requirement `POPPY-3.4`.

- [ ] **Step 7: `ValidatePoppyDelegation`**

Test on fixtures: valid passes and exports `poppy_delegation_claims` and `poppy_delegated_public_jwk`; `poppy_dlg` header absent fails ("Session JWT without a delegation"); delegation `kid` not in `client.jwks` fails; signature by another key fails; `typ` `JWT` fails; delegation `alg` `RS256` fails and `alg` `none` fails (§5.2 "`alg` is `ES256` or `PS384`"); delegation `iss` ≠ Session JWT `iss` fails; missing `jti` fails; missing `iat` or `exp` fails; `exp` in the past fails; `exp - iat` = 86401 fails; 86400 passes; `cnf.jwk` containing `d` fails; a delegation carrying `sub`, `aud` or `agent_id` fails (§5.2 "MUST NOT put a User ID, an Agent ID, a Company's issuer, or anything else about the User"; only these named claims are detectable); delegation `alg` `PS384` signed by an RSA key in `client.jwks` passes.
Implement: `@PreEnvironment(required = "client", strings = "poppy_session_jwt") @PostEnvironment(required = {"poppy_delegation_claims", "poppy_delegated_public_jwk"})`; it parses the Session JWT header, and its payload without verification solely to read `iss` for the comparison (the signature is verified in `ValidatePoppySessionJwt`), then verifies the delegation fully. Requirement `POPPY-5.2`.

- [ ] **Step 8: `ValidatePoppySessionJwt`**

Test on fixtures with `incoming_request.method` `GET`, `request_url` `https://h:8443/test/a/root-well-known/api/me?x=1`, `issuer`, `incoming_access_token` and `access_token` both `tok`: valid passes and exports `poppy_session_claims`; signature by a different delegated key fails; `kid` ≠ thumbprint of `cnf.jwk` fails; `typ` wrong fails; `alg` `ES384` fails; `aud` = token endpoint URL fails; `aud` as a one-element array fails; `htm` `POST` fails; `htu` with the `?x=1` query fails; `htu` with a different path fails; `iat` 120 s ago fails; `iat` 61 s in the future fails; `iat` 30 s either way passes; `ath` missing with a token fails; `ath` for another token fails; `ath` present with no `incoming_access_token` fails; `iss` ≠ `poppy_incoming_client_id` fails; missing `sid`, `sub` or `jti` fails; `incoming_access_token` ≠ `access_token` (a token the suite did not issue) fails.
Implement: `@PreEnvironment(required = {"incoming_request", "poppy_delegated_public_jwk"}, strings = {"poppy_session_jwt", "issuer", "poppy_incoming_client_id"}) @PostEnvironment(required = "poppy_session_claims")`; the acceptance window is 60 s each way. Requirement `POPPY-5.2`, `POPPY-5.3`.

- [ ] **Step 9: `EnsurePoppyNoCredentialsInQuery`**

Test: `query_string_params` containing `access_token` or `poppy_session` fails; empty passes.
Requirement `POPPY-5.6`.

- [ ] **Step 10: Run the tests and commit**

Run: `mvn test -Dtest='net.openid.conformance.poppy.condition.*_UnitTest'`
Expected: PASS, 36 classes.

```bash
git add src/main/java/net/openid/conformance/poppy/condition src/test/java/net/openid/conformance/poppy/condition
git commit -m "Poppy: authorization, client assertion and Session header verification for the suite acting as a Company"
```

---

### Task 7: Agent test module and plan

**Files:**
- Create: `src/main/java/net/openid/conformance/poppy/agent/AbstractPoppyAgentTestModule.java`, `PoppyAgentHappyFlow.java`, `PoppyAgentTestPlan.java`
- Reference while writing: `src/main/java/net/openid/conformance/fapi2spfinal/AbstractFAPI2SPFinalClientTest.java:769-800` (well-known), `:1352-1420` (token endpoint request bookkeeping), `:1564-1640` (authorization endpoint), `:1847-1900` (resource endpoint); `src/main/java/net/openid/conformance/openid/client/AbstractOIDCCClientTest.java:473-600` (path routing).

**Interfaces:**
- Consumes: Tasks 5 and 6, plus existing `condition.as`: `CreateEffectiveAuthorizationRequestParameters`, `EnsureResponseTypeIsCode`, `EnsureMatchingClientId`, `EnsureValidRedirectUriForAuthorizationEndpointRequest`, `ExtractRequestedScopes` (if OIDC-free; else skip), `CreateAuthorizationCode`, `CreateAuthorizationEndpointResponseParams`, `AddCodeToAuthorizationEndpointResponseParams`, `AddIssToAuthorizationEndpointResponseParams`, `SendAuthorizationResponseWithResponseModeQuery`, `ExtractClientAssertion`, `EnsureClientAssertionTypeIsJwt`, `FetchClientKeys`, `ValidateClientAssertionSignature`, `ValidateClientAssertionClaims`, `ValidateClientAssertionAudClaimIsIssuerAsString`, `CheckForClientAssertionJtiReuse`, `ValidateAuthorizationCode`, `ValidateRedirectUriForTokenEndpointRequest`, `CheckPkceCodeVerifier`, `GenerateBearerAccessToken`, `CreateRefreshToken`, `CreateTokenEndpointResponse`; `condition.rs`: `ExtractBearerAccessTokenFromHeader`, `EnsureIncomingRequestMethodIsGet`.
- Produces: module `poppy-agent-happy-flow`, plan `poppy-agent-test-plan`, URLs `https://<host>/.well-known/poppy.json`, `https://<host>/.well-known/oauth-protected-resource`, `https://<host>/.well-known/oauth-authorization-server/test/a/root-well-known`, `<issuer>/authorize`, `<issuer>/token`, `<issuer>/openapi.json`, `<issuer>/api/me`.

- [ ] **Step 1: `AbstractPoppyAgentTestModule extends AbstractTestModule`**

`@ConfigurationFields({"poppy.company_domain"})`. `configure`: `env.putObject("config", config)`; `env.putString("base_url", baseUrl)`; `EnsurePoppyRootWellKnownAlias`; `GeneratePoppyCompanyDocuments`; `exposeEnvString("issuer")`; `setStatus(CONFIGURED)`; `fireSetupDone()`.

`start()`: `setStatus(WAITING)` and log the instruction "Point the Personal Agent at Company domain `<poppy_domain>`".

`handleWellKnown(path, ...)`: `/.well-known/poppy.json` → `poppy_json`; `/.well-known/oauth-protected-resource` → `poppy_protected_resource_metadata`; path starting `/.well-known/oauth-authorization-server` → `server`; else `super.handleWellKnown`. All `application/json`, 200. Each handler runs inside a `startBlock`/`endBlock` with `env.mapKey("incoming_request", requestId)` as the FAPI2 client base does, so the fetch appears in the log.

`handleHttp(path, ...)`: first `String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37); env.putObject(requestId, requestParts);` as `AbstractFAPI2SPFinalClientTest.java:751-753` does; each endpoint handler opens its block with `call(exec().startBlock("...").mapKey("incoming_request", requestId))` and ends it with `call(exec().unmapKey("incoming_request").endBlock())`, so `ExtractBearerAccessTokenFromHeader`, `ExtractPoppySessionHeaders`, `EnsurePoppyNoCredentialsInQuery` and `ValidatePoppySessionJwt` read the request they belong to (`:1860` for the resource endpoint). `handleWellKnown` does the same. Then: `authorize` → `authorizationEndpoint(requestId)`; `token` → `tokenEndpoint(requestId)`; `openapi.json` → a constant minimal OpenAPI 3.1 document describing `GET /api/me` with an `oauth2` security scheme whose `authorizationCode` flow lists scope `orders:read`; `api/me` → `apiEndpoint(requestId)`; else `super.handleHttp` (404 and report, per `unexpectedHttpRequest`).

`authorizationEndpoint`: `setStatus(RUNNING)`; copy `query_string_params` to `authorization_endpoint_http_request_params`; `CreateEffectiveAuthorizationRequestParameters`; put `effective...client_id` into `poppy_incoming_client_id`; `FetchPoppyClientMetadataDocument`; `ValidatePoppyClientMetadataDocument`; `CheckForUnexpectedFieldsInPoppyClientMetadataDocument` at `WARNING`; `FetchPoppyClientJwks`; `EnsurePoppyClientJwksContainsOnlyPublicKeys`; `EnsureResponseTypeIsCode`; `EnsurePoppyRedirectUriMatchesClientMetadata`; `EnsurePoppyAuthorizationRequestContainsS256CodeChallenge`; `EnsurePoppyAuthorizationRequestContainsScope`; `CreateAuthorizationCode`; `CreateAuthorizationEndpointResponseParams`; `AddCodeToAuthorizationEndpointResponseParams`; `AddIssToAuthorizationEndpointResponseParams`; `SendAuthorizationResponseWithResponseModeQuery`; `setStatus(WAITING)`; return the redirect the last condition stored in `authorization_endpoint_response_redirect` (see how the FAPI2 client base turns it into a `RedirectView`).

`tokenEndpoint`: `setStatus(RUNNING)`; `call(exec().startBlock("Token endpoint").mapKey("token_endpoint_request", requestId))` where `requestId` names the stored full `requestParts` object, exactly as `AbstractFAPI2SPFinalClientTest.java:1356` does, because `ExtractClientAssertion:20`, `EnsureClientAssertionTypeIsJwt:15`, `ValidateAuthorizationCode:15` and `ValidateCodeVerifierWithS256:18` all read `token_endpoint_request.body_form_params.*`; unmap at the end of the block. Then `ExtractClientAssertion`; `EnsureClientAssertionTypeIsJwt`; `EnsurePoppyClientAssertionAlgIsAdvertised`; `ValidateClientAssertionSignature`; `ValidateClientAssertionClaims`; `ValidateClientAssertionAudClaimIsIssuerAsString` with `.requirement("POPPY-4.1")`; `EnsurePoppyClientAssertionContainsIat`; `EnsurePoppyClientAssertionIsShortLived` at `WARNING`; `CheckForClientAssertionJtiReuse` at `WARNING`; `ValidateAuthorizationCode`; `ValidateRedirectUriForTokenEndpointRequest`; `call(sequence(CheckPkceCodeVerifier.class))` (it is a `ConditionSequence`, not a condition: `condition/as/CheckPkceCodeVerifier.java:9`); `GenerateBearerAccessToken`; `CreateRefreshToken`; `env.putString("access_token_expiration", "3600")` (the string `CreateTokenEndpointResponse.java:22` reads to emit numeric `expires_in`); read `CreateTokenEndpointResponse` for the key it uses for `scope` and set it to the requested scope; `CreateTokenEndpointResponse`; `setStatus(WAITING)`; return `token_endpoint_response` 200 JSON.

`apiEndpoint`: `setStatus(RUNNING)`; `EnsureIncomingRequestMethodIsGet`; `EnsurePoppyNoCredentialsInQuery`; `ExtractBearerAccessTokenFromHeader`; `ExtractPoppySessionHeaders`; `EnsurePoppyVersionIsSupported`; `ValidatePoppyDelegation`; `ValidatePoppySessionJwt`; respond `200` `{"user": "<sub claim>", "scope": "orders:read"}`; then `fireTestFinished()`. A missing or unverifiable Session header is a `callAndStopOnFailure` FAILURE (the API's response to a real agent would be `invalid_session`, but the module ends there: the behaviour under test was not exercised).

- [ ] **Step 2: `PoppyAgentHappyFlow` and `PoppyAgentTestPlan`**

`@PublishTestModule(testName = "poppy-agent-happy-flow", displayName = "Poppy Personal Agent: happy flow (Direct Sign-In, Bearer token, Session header)", summary = "The suite acts as a Company at the host root: it serves poppy.json, protected resource metadata and OAuth server metadata, signs the Personal Agent in with Direct Sign-In (PKCE S256, private_key_jwt), and verifies the Poppy-Session and Poppy-Version headers on one API call. The test configuration alias must be 'root-well-known' so that /.well-known/poppy.json is served at the host root; one such test can run at a time on a host.", profile = "Poppy-0.4")`.

`@PublishTestPlan(testPlanName = "poppy-agent-test-plan", displayName = "Poppy 0.4 Personal Agent test plan (alpha, not part of a certification program)", profile = TestPlan.ProfileNames.poppyAgentTest, specFamily = TestPlan.SpecFamilyNames.poppy, testModules = {PoppyAgentHappyFlow.class})`.

- [ ] **Step 3: Compile, run ArchUnit and the full unit suite**

Run: `mvn test -Dtest='*ArchUnit*,net.openid.conformance.poppy.**'` then `mvn test`
Expected: PASS.

- [ ] **Step 4: Manual pairing smoke test**

Start the dev profile (`mvn spring-boot:run -Dspring-boot.run.profiles=dev`, MongoDB and nginx up per AGENTS.md). With the local nginx also listening on 443 (Task 8 Step 3 note), create the Agent plan with config `{"alias": "root-well-known", "description": "poppy smoke", "poppy": {"company_domain": "localhost.emobix.co.uk"}}`, start the module, confirm `curl -sk https://localhost.emobix.co.uk/.well-known/poppy.json` returns the document with `organization.domain` `localhost.emobix.co.uk`. Create the Company plan with the Task 8 Company config (substituting `{BASEURL}` by hand), run it, and read both logs.
Expected: both modules finish PASSED (warnings allowed only for `CheckForRefreshTokenValue` if the emulated Company omits a refresh token, which it must not).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/openid/conformance/poppy/agent
git commit -m "Poppy: Personal Agent test plan with a Direct Sign-In happy flow served at the host root"
```

---

### Task 8: CI pairing

**Files:**
- Create: `scripts/test-configs-rp-against-op/poppy-company-test-config.json`, `scripts/test-configs-rp-against-op/poppy-agent-test-config.json`, `.gitlab-ci/expected-failures-poppy.json` (`[]`), `.gitlab-ci/expected-skips-poppy.json` (`[]`)
- Modify: `nginx/nginx.conf:33` (the CI nginx image, built from `./nginx` by `docker-compose-localtest.yml`): add `listen 443 ssl;` beside `listen 8443 ssl;` in the same server block, so `https://nginx/.well-known/poppy.json` reaches the suite inside the docker network with no port (no `ports:` mapping needed for container-to-container traffic). Leave `X-Forwarded-Port` alone.
- Modify: `.gitlab-ci/run-tests.sh` (new `makePoppyTests` function and `--poppy-tests` branch modelled on `--authzen-tests` at lines 1035-1043; usage line 1093; the `EXPECTED_FAILURES_FILE` and `EXPECTED_SKIPS_FILE` pipe-lists at lines 29-30), `scripts/run-test-plan.py:1476-1560` (classification: `poppy = all_test_modules[m]['profile'] in ['Poppy-0.4']`, excluded from every other `show_untested` branch, new branch `elif show_untested == 'poppy'`), `.gitlab-ci.yml` (`run_poppy_test_plan` function beside `run_vc_test_plan` at line 1095 and a `poppy_test` job beside `vc_test` at line 495), `AGENTS.md` options list under "Running integration tests".
- Test: `.gitlab-ci/run-tests.sh --poppy-tests --list` then `scripts/run-integration-tests.sh --poppy-tests`.

**Interfaces:**
- Consumes: both plans from Tasks 4 and 7; the pairing grammar `OUTER[...]:module{INNER[...]:module}inner-config outer-config` where the outer plan is started first and the inner plan is run while the outer module is `WAITING` (`run-test-plan.py:289-318`).
- Produces: `TESTS="${TESTS} poppy-agent-test-plan:poppy-agent-happy-flow{poppy-company-test-plan:poppy-company-happy-flow}${CONFIGS}/poppy-company-test-config.json ${CONFIGS}/poppy-agent-test-config.json"` with `CONFIGS="${SUITE_DIR}/scripts/test-configs-rp-against-op"`.

- [ ] **Step 1: Configs**

`poppy-agent-test-config.json`: `{"alias": "root-well-known", "description": "Poppy Agent test (suite as Company) paired with the Company test", "poppy": {"company_domain": "{HOSTNAME}"}}` (`{HOSTNAME}` is `nginx` in CI, `run-test-plan.py:161`).

`poppy-company-test-config.json`: `alias` `oidf-poppy-company-test`; `description`; `poppy.company_domain` `"{HOSTNAME}"`; `client.scope` `"orders:read"`; `client.jwks.keys` one ES256 private key with `kid` `poppy-agent-es256-1` generated for this purpose (test key; it will appear in logs, see AGENTS.md "Deliberate non-features"); `resource.resourceUrl` `"{BASEURL}test/a/root-well-known/api/me"`; `browser` block copied from `fapi-au-connectid-op-test-config.json` with `match` `https://*/test/a/root-well-known/authorize*` and the task's `match` `https://*/test/a/oidf-poppy-company-test/callback*`.

- [ ] **Step 2: run-tests.sh, run-test-plan.py, .gitlab-ci.yml, AGENTS.md**

Add the pieces listed under Files. In `run-test-plan.py`, the `show_untested == 'poppy'` branch removes every module that is not `poppy`; every other branch's exclusion list gains `or poppy`. The `.gitlab-ci.yml` job:

```yaml
poppy_test:
  <<: *deployment_test
  rules:
    - if: $CI_COMMIT_BRANCH
      when: on_success
  script:
    - set_up_for_running_test_plan
    - run_poppy_test_plan
```

- [ ] **Step 3: List and run locally**

Run: `.gitlab-ci/run-tests.sh --poppy-tests --list`
Expected: `  1. poppy-agent-happy-flow {poppy-company-happy-flow} [poppy-agent-test-config.json / poppy-company-test-config.json]` (outer config first, `run-test-plan.py:1220-1226`).

Run: `./scripts/run-integration-tests.sh --poppy-tests` (prints a log path under `/tmp/`; read it). Locally this needs the devenv nginx to also listen on 443 for `localhost.emobix.co.uk` (add `listen 443 ssl;` to its server block; binding 443 needs root, so do this once by hand and note it in the MR); without it the domain checks fail, as they must.
Expected: both modules PASSED; "untested modules" list empty for the poppy family; exit status 0; no entries needed in the expected-failures file. If a warning or failure is genuine emulator non-compliance, fix the emulator; never weaken a check (AGENTS.md "Emulated side vs side under test").

- [ ] **Step 4: Commit**

```bash
git add scripts/test-configs-rp-against-op/poppy-*.json .gitlab-ci/expected-failures-poppy.json .gitlab-ci/expected-skips-poppy.json .gitlab-ci/run-tests.sh scripts/run-test-plan.py .gitlab-ci.yml AGENTS.md nginx/nginx.conf
git commit -m "CI: pair the Poppy Agent and Company happy flows against each other"
```

---

### Task 9: Whole-branch review, tidy, pipeline

**Files:** none new.

- [ ] **Step 1: Full build and tests**

Run: `mvn test` (PMD and checkstyle on) and, under the Node 24 nix shell, `npm run test:ci` in `frontend/` if Task 4 touched a catalog test.
Expected: PASS.

- [ ] **Step 2: Independent review**

Dispatch one reviewer (default model, not `sonnet`) with the `openid-review` skill over `git diff origin/master...poppy-happy-flow`, giving it the spec snapshot path and this plan, and asking specifically: (a) is any validator relaxed to suit the emulator, (b) does every FAILURE cite a MUST on the side under test, (c) does any condition use `getAs*`, (d) are the env-key contracts in this plan's Interfaces blocks what the code actually reads and writes.

- [ ] **Step 3: Tidy and pipeline**

Fold fixups with `git commit -m "fixup! <subject>"` and `git -c sequence.editor=true rebase -i --autosquash origin/master`. Then stop: the controller reports to Joseph and asks before any push (see memory `feedback-no-unprompted-pushes`). When pushing is approved, one pipeline at most alongside whatever else is running (memory `feedback-max-two-ci-pipelines`); watch `poppy_test` plus `compare_*` jobs.

---

## Known limitations recorded for the MR description

- One Agent test at a time per host (alias `root-well-known`). Decided 2026-11-05: ship this for now. The follow-up is a general per-test host facility, not a Poppy one: `<alias>.test.certification.openid.net` in production (`<alias>.test-demo.` and `<alias>.test-staging.` for the other environments), the suffix set by a Helm value, routed by `X-Forwarded-Host` in the dispatcher with every path on that host handed to the aliased test (`/.well-known/*` to `handleWellKnown`, the rest to `handleHttp`), the fixed alias kept as the no-suffix fallback. Wildcard DNS already exists; the new pieces are a Let's Encrypt wildcard certificate per suffix via a cert-manager DNS-01 solver (needs API credentials for the `certification.openid.net` zone), an ingress-nginx wildcard host rule, alias lowercasing and DNS-label validation, and `Cache-Control: no-store` on root-served documents. Review apps stay on the fixed alias. Other candidates for the facility: OP tests needing a root issuer, Federation entities, MCP servers.
- A Company reachable only on a non-default HTTPS port cannot be tested, because §3.1 and §3.2 give `organization.domain` no place for a port. Local runs of the pairing need nginx listening on 443 (`devenv` nginx config), otherwise the three domain checks fail, which is correct behaviour, not something to relax.
- Requirement links land on the spec page top (slug anchors). Mapping table is a follow-up.
- No 0.4 implementation exists outside this pairing; the first external check is pointing `poppy-company-happy-flow` at the `adewale/pda` strict mock Company, which is on 0.3 and will reject or ignore `Poppy-Version` (analysis 6.5).
