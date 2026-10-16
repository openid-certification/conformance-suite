package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Locale;

/**
 * Section 11.3 — a 401 response from the PDP SHOULD include a `WWW-Authenticate`
 * response header indicating the expected scheme and realm. Caller decides the
 * severity (typically WARNING since this is SHOULD).
 */
public class EnsureAuthzenApiResponseHasWwwAuthenticate extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response")
	public Environment evaluate(Environment env) {
		Integer status = env.getInteger("authzen_api_endpoint_response", "status");
		if (status == null) {
			throw error("AuthZEN API response has no HTTP status; cannot evaluate the Section 11.3 WWW-Authenticate SHOULD check");
		}
		if (status != 401) {
			log("Response status is not 401; the WWW-Authenticate SHOULD check applies only to 401 responses",
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
				// No HTTP authentication scheme names a TLS client certificate, so the header has no
				// standard challenge to carry; the handshake refusal these modules accept avoids the 401.
				throw error("PDP returned 401 without a WWW-Authenticate response header (Section 11.3 SHOULD). With mTLS there is no "
						+ "standard HTTP authentication scheme for the header to name; a PDP that refuses a missing or untrusted client "
						+ "certificate in the TLS handshake, which this test also accepts, sends no 401 at all",
					args("response_headers", headers));
			}
			throw error("PDP returned 401 without a WWW-Authenticate response header (Section 11.3 SHOULD)",
				args("response_headers", headers));
		}
		logSuccess("PDP returned a WWW-Authenticate response header on 401");
		return env;
	}
}
