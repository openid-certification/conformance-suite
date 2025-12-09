package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureDecisionResponseTrue extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_decision")
	public Environment evaluate(Environment env) {

		JsonObject decision = env.getObject("authzen_api_endpoint_decision");
		if (decision == null) {
			throw error("Decision response not found");
		}
		if(!decision.has("decision")) {
			throw error("Decision response does not contain decision value");
		}
		if(!OIDFJSON.getBoolean(decision.get("decision"))) {
			throw error("Decision response is not true", args("decision", decision));
		}
		logSuccess("Decision response is true", args("decision", decision));

		return env;
	}

}
