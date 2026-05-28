package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
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
	public Environment evaluate(Environment env) {
		if (env.getString("authzen_idempotency_first_response_body") != null) {
			return env;
		}
		JsonObject body = (JsonObject) env.getElementFromObject("authzen_api_endpoint_response", "body_json");
		if (body == null) {
			throw error("No JSON response body available to capture for idempotency check");
		}
		env.putString("authzen_idempotency_first_response_body", body.toString());
		logSuccess("Captured initial response body for idempotency check", args("body", body));
		return env;
	}
}
