package net.openid.conformance.openid.ssf.conditions;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

/**
 * Asserts that a rejected resource request carries an RFC 6750 section 3 {@code WWW-Authenticate}
 * challenge, as the CAEP Interop Profile (2.7.2) requires: "If the access token is not sufficient
 * for the requested action, the Resource Server MUST return errors as per Section 3.1 of
 * [RFC6750]".
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
					+ "The CAEP Interop Profile (2.7.2) requires errors as per RFC 6750 section 3.1.",
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
