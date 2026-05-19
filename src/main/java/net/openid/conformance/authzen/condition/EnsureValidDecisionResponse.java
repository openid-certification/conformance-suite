package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureValidDecisionResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_decision")
	public Environment evaluate(Environment env) {
		JsonObject decisionObj = env.getObject("authzen_api_endpoint_decision");
		if(!decisionObj.has("decision")) {
			throw error("Decision response does not contain decision value");
		}
		JsonElement decisionElement = decisionObj.get("decision");
		if(!decisionElement.isJsonPrimitive()) {
			throw error("Decision value is not a primitive", args("decision", decisionObj));
		}
		JsonPrimitive jsonPrimitive = decisionElement.getAsJsonPrimitive();
		if(!jsonPrimitive.isBoolean()) {
			throw error("Decision value is not a boolean value", args("decision", decisionObj));
		}
		logSuccess("Decision response is valid");
		return env;
	}

}
