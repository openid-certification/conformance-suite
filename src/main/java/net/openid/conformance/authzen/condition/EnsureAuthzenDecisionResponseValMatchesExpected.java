package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureAuthzenDecisionResponseValMatchesExpected extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_decision_endpoint_expected_response", "authzen_api_endpoint_decision"})
	public Environment evaluate(Environment env) {
		boolean expectedDecision = readBooleanDecision(env, "authzen_decision_endpoint_expected_response", "expected");
		boolean actualDecision = readBooleanDecision(env, "authzen_api_endpoint_decision", "actual");
		if (expectedDecision != actualDecision) {
			throw error("Decision response value does not match expected value", args("expected", expectedDecision, "actual", actualDecision));
		}
		logSuccess("Decision response value matches expected", args("decision", expectedDecision));
		return env;
	}

	private boolean readBooleanDecision(Environment env, String objectKey, String role) {
		JsonObject obj = env.getObject(objectKey);
		if (obj == null) {
			throw error(role + " decision response is missing from the environment", args("env_key", objectKey));
		}
		if (!obj.has("decision")) {
			throw error(role + " decision response does not contain a `decision` element", args(role, obj));
		}
		JsonElement decision = obj.get("decision");
		if (!decision.isJsonPrimitive() || !decision.getAsJsonPrimitive().isBoolean()) {
			throw error(role + " decision response `decision` is not a boolean",
				args(role, obj, "value", decision));
		}
		return OIDFJSON.getBoolean(decision);
	}
}
