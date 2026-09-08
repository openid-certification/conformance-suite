package net.openid.conformance.openid.ssf.conditions;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

/**
 * Asserts that a rejected resource request carries an RFC 6750 section 3 {@code WWW-Authenticate}
 * challenge. A bare {@code WWW-Authenticate: Bearer} without an error code is accepted - for a
 * request without any authentication information, RFC 6750 section 3.1 even says the server
 * SHOULD NOT include an error code.
 * <p>
 * Note on severity: the {@code WWW-Authenticate} requirement itself comes from RFC 6750
 * section 3 ("MUST include the HTTP WWW-Authenticate response header field"). The CAEP Interop
 * Profile (2.7.2) only cites section 3.1, which defines the error codes, so the profile's
 * normative chain to the header is imprecise. Callers therefore invoke this condition at
 * WARNING severity until the profile is editorially clarified to reference RFC 6750
 * sections 3 and 3.1 - the rejection itself (401/403) remains the FAILURE-level check.
 * <p>
 * Expects the response under {@code endpoint_response} (map {@code resource_endpoint_response_full}
 * onto it before calling).
 */
public class OIDSSFEnsureWwwAuthenticateHeaderPresent extends AbstractCondition {

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		JsonElement headersEl = env.getElementFromObject("endpoint_response", "headers");
		if (headersEl == null || !headersEl.isJsonObject()) {
			throw error("Could not find the response headers of the rejected request",
				args("endpoint_response", env.getObject("endpoint_response")));
		}

		String challenge = null;
		for (Map.Entry<String, JsonElement> header : headersEl.getAsJsonObject().entrySet()) {
			if ("www-authenticate".equalsIgnoreCase(header.getKey())) {
				challenge = header.getValue().isJsonArray() && !header.getValue().getAsJsonArray().isEmpty()
					? OIDFJSON.tryGetString(header.getValue().getAsJsonArray().get(0))
					: OIDFJSON.tryGetString(header.getValue());
				break;
			}
		}

		if (challenge == null || challenge.isBlank()) {
			throw error("The rejected request did not carry a 'WWW-Authenticate' response header. "
					+ "RFC 6750 section 3 requires bearer-token resource servers to include a 'WWW-Authenticate' challenge "
					+ "when rejecting a request; the CAEP Interop Profile (2.7.2) requires errors as per RFC 6750 section 3.1, "
					+ "which are carried in that header.",
				args("response_headers", headersEl));
		}

		if (!challenge.regionMatches(true, 0, "Bearer", 0, "Bearer".length())) {
			throw error("The 'WWW-Authenticate' response header does not contain a 'Bearer' challenge (RFC 6750 section 3)",
				args("www_authenticate", challenge));
		}

		logSuccess("The rejected request carried a Bearer 'WWW-Authenticate' challenge",
			args("www_authenticate", challenge));

		return env;
	}
}
