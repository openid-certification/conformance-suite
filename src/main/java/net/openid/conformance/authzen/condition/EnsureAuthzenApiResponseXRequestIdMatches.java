package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Locale;

/**
 * Asserts the PDP echoed the X-Request-ID supplied on the request back in the
 * response headers (Section 10.1.3-4 — exact-echo when PEP supplied an
 * identifier).
 */
public class EnsureAuthzenApiResponseXRequestIdMatches extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response", strings = "authzen_api_endpoint_request_x_request_id")
	public Environment evaluate(Environment env) {
		String expected = env.getString("authzen_api_endpoint_request_x_request_id");
		JsonObject headers = (JsonObject) env.getElementFromObject("authzen_api_endpoint_response", "headers");
		if (headers == null) {
			throw error("No response headers captured for Authzen API response");
		}

		String actual = findHeaderCaseInsensitive(headers, "X-Request-ID");
		if (actual == null) {
			throw error("PDP did not echo X-Request-ID in the response headers",
				args("expected", expected, "response_headers", headers));
		}
		if (!expected.equals(actual)) {
			throw error("PDP did not return the same X-Request-ID supplied on the request",
				args("expected", expected, "actual", actual));
		}

		logSuccess("PDP echoed X-Request-ID correctly", args("X-Request-ID", actual));
		return env;
	}

	private static String findHeaderCaseInsensitive(JsonObject headers, String name) {
		String lower = name.toLowerCase(Locale.ROOT);
		for (String key : headers.keySet()) {
			if (key.toLowerCase(Locale.ROOT).equals(lower)) {
				JsonElement value = headers.get(key);
				if (value.isJsonArray() && !value.getAsJsonArray().isEmpty()) {
					return OIDFJSON.getString(value.getAsJsonArray().get(0));
				}
				if (value.isJsonPrimitive()) {
					return OIDFJSON.getString(value);
				}
			}
		}
		return null;
	}
}
