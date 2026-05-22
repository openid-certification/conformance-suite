package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Locale;

/**
 * Spec 11.3-2 — a 401 response from the PDP SHOULD include a `WWW-Authenticate`
 * response header indicating the expected scheme and realm. Caller decides the
 * severity (typically WARNING since this is SHOULD).
 */
public class EnsureAuthzenApiResponseHasWwwAuthenticate extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject headers = (JsonObject) env.getElementFromObject("authzen_api_endpoint_response", "headers");
		if (headers == null) {
			throw error("No response headers captured for Authzen API response");
		}
		boolean found = false;
		for (String key : headers.keySet()) {
			if (key.toLowerCase(Locale.ROOT).equals("www-authenticate")) {
				found = true;
				break;
			}
		}
		if (!found) {
			throw error("PDP returned 401 without a WWW-Authenticate response header (spec 11.3-2 SHOULD)",
				args("response_headers", headers));
		}
		logSuccess("PDP returned a WWW-Authenticate response header on 401");
		return env;
	}
}
