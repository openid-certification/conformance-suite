package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Asserts that the decision returned by the current Authzen API response equals
 * the decision captured by {@link CaptureDecisionForIdempotencyCheck} on the
 * first iteration of an idempotency test loop.
 */
public class EnsureDecisionMatchesIdempotencyCheck extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_decision", strings = "authzen_idempotency_first_decision")
	public Environment evaluate(Environment env) {
		boolean expected = Boolean.parseBoolean(env.getString("authzen_idempotency_first_decision"));
		boolean actual = readBooleanDecision(env);
		if (expected != actual) {
			throw error("Decision changed across consecutive identical requests — PDP is not idempotent",
				args("first_iteration_decision", expected, "current_iteration_decision", actual));
		}
		logSuccess("Decision matched first iteration", args("decision", actual));
		return env;
	}

	private boolean readBooleanDecision(Environment env) {
		JsonObject decision = env.getObject("authzen_api_endpoint_decision");
		if (decision == null) {
			throw error("Current iteration decision response is missing from the environment");
		}
		if (!decision.has("decision")) {
			throw error("Current iteration decision response does not contain a `decision` element",
				args("decision_response", decision));
		}
		JsonElement field = decision.get("decision");
		if (!field.isJsonPrimitive() || !field.getAsJsonPrimitive().isBoolean()) {
			throw error("Current iteration decision response `decision` is not a boolean",
				args("decision_response", decision, "value", field));
		}
		return OIDFJSON.getBoolean(field);
	}
}
