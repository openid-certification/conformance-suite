package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Captures the boolean decision from the current Authzen API response under the
 * env key `authzen_idempotency_first_decision` if not already set. Subsequent
 * iterations should call {@link EnsureDecisionMatchesIdempotencyCheck} to assert
 * that the decision did not change.
 */
public class CaptureDecisionForIdempotencyCheck extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_decision")
	@PostEnvironment(strings = "authzen_idempotency_first_decision")
	public Environment evaluate(Environment env) {
		if (env.getString("authzen_idempotency_first_decision") != null) {
			log("First-iteration decision already captured; skipping");
			return env;
		}
		boolean value = readBooleanDecision(env);
		env.putString("authzen_idempotency_first_decision", Boolean.toString(value));
		logSuccess("Captured initial decision for idempotency check", args("decision", value));
		return env;
	}

	private boolean readBooleanDecision(Environment env) {
		JsonObject decision = env.getObject("authzen_api_endpoint_decision");
		if (decision == null) {
			throw error("Decision response is missing from the environment");
		}
		if (!decision.has("decision")) {
			throw error("Decision response does not contain a `decision` element", args("decision_response", decision));
		}
		JsonElement field = decision.get("decision");
		if (!field.isJsonPrimitive() || !field.getAsJsonPrimitive().isBoolean()) {
			throw error("Decision response `decision` is not a boolean",
				args("decision_response", decision, "value", field));
		}
		return OIDFJSON.getBoolean(field);
	}
}
