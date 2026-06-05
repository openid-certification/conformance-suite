package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Captures the JSON body of the current Authzen API response under the env key
 * {@code authzen_idempotency_first_response_body} if not already set. Subsequent
 * iterations should call {@link EnsureAuthzenResponseBodyMatchesIdempotencyCheck}
 * to assert the body did not change.
 */
public class CaptureAuthzenResponseBodyForIdempotencyCheck extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response")
	@PostEnvironment(strings = "authzen_idempotency_first_response_body")
	public Environment evaluate(Environment env) {
		if (env.getString("authzen_idempotency_first_response_body") != null) {
			log("First-iteration response body already captured; skipping");
			return env;
		}
		JsonElement bodyElem = env.getElementFromObject("authzen_api_endpoint_response", "body_json");
		if (bodyElem == null) {
			throw error("No JSON response body available to capture for idempotency check");
		}
		if (!bodyElem.isJsonObject()) {
			throw error("Response body is not a JSON object; cannot capture for idempotency check",
				args("body_json", bodyElem));
		}
		JsonObject body = bodyElem.getAsJsonObject();
		env.putString("authzen_idempotency_first_response_body", body.toString());
		logSuccess("Captured initial response body for idempotency check", args("body", body));
		return env;
	}
}
