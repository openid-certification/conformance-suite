package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Locale;

/**
 * Section 11.3 — a 401 response from the PDP SHOULD include a `WWW-Authenticate`
 * response header indicating the expected scheme and realm. Caller decides the
 * severity (typically WARNING since this is SHOULD). Under mtls a missing header is only logged:
 * the header names an HTTP authentication scheme, and none exists for a TLS client certificate.
 */
public class EnsureAuthzenApiResponseHasWwwAuthenticate extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response")
	public Environment evaluate(Environment env) {
		Integer status = env.getInteger("authzen_api_endpoint_response", "status");
		if (status == null) {
			throw error("AuthZEN API response has no HTTP status, so the WWW-Authenticate check cannot be made");
		}
		if (status != 401) {
			log("Response status is not 401; the WWW-Authenticate check applies only to 401 responses",
				args("status", status));
			return env;
		}
		JsonObject headers = (JsonObject) env.getElementFromObject("authzen_api_endpoint_response", "headers");
		if (headers == null) {
			throw error("No response headers captured for AuthZEN API response");
		}
		boolean found = false;
		for (String key : headers.keySet()) {
			if (key.toLowerCase(Locale.ROOT).equals("www-authenticate")) {
				found = true;
				break;
			}
		}
		if (!found) {
			if ("mtls".equals(env.getString("client_auth_type"))) {
				// No HTTP authentication scheme names a TLS client certificate, so a PDP that answers a
				// missing or untrusted certificate with a 401 has no standard challenge to send.
				log("PDP returned 401 without a WWW-Authenticate response header. With mTLS there is no standard HTTP "
						+ "authentication scheme for the header to name, so it is not expected",
					args("response_headers", headers));
				return env;
			}
			String keyHeader = "api_key".equals(env.getString("client_auth_type"))
				? env.getString("client", "api_key_header") : null;
			if (keyHeader != null && !keyHeader.isBlank() && !"authorization".equalsIgnoreCase(keyHeader.trim())) {
				// No registered HTTP authentication scheme names a key in a header of its own, but the
				// scheme in a challenge only has to be a token, so the PDP can name one.
				throw error("PDP returned 401 without a WWW-Authenticate response header. The API key is sent in the "
						+ keyHeader.trim() + " header, for which no standard HTTP authentication scheme exists; a challenge "
						+ "naming a scheme of the PDP's own, such as ApiKey, satisfies this check",
					args("response_headers", headers, "api_key_header", keyHeader.trim()));
			}
			throw error("PDP returned 401 without a WWW-Authenticate response header",
				args("response_headers", headers));
		}
		logSuccess("PDP returned a WWW-Authenticate response header on 401");
		return env;
	}
}
