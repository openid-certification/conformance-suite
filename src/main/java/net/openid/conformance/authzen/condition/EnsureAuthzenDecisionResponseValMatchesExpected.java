package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureAuthzenDecisionResponseValMatchesExpected extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_decision_endpoint_expected_response", "authzen_api_endpoint_decision"})
	public Environment evaluate(Environment env) {
		JsonObject expected = env.getObject("authzen_decision_endpoint_expected_response");
		JsonObject actual = env.getObject("authzen_api_endpoint_decision");
		boolean expectedDecision = OIDFJSON.getBoolean(expected.get("decision"));
		if (!actual.has("decision")) {
			throw error("Actual decision response does not contain a decision element", args("actual", actual));
		}
		boolean actualDecision = OIDFJSON.getBoolean(actual.get("decision"));
		if (expectedDecision != actualDecision) {
			throw error("Decision response value does not match expected value", args("expected", expectedDecision, "actual", actualDecision));
		}
		logSuccess("Decision response value matches expected", args("decision", expectedDecision));
		return env;
	}
}
