package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractAuthzenApiEndpointDecisionResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response")
	@PostEnvironment(required = "authzen_api_endpoint_decision")
	public Environment evaluate(Environment env) {

		JsonObject decision = (JsonObject) env.getElementFromObject("authzen_api_endpoint_response", "body_json");
		if (decision == null) {
			throw error("No json response from Authzen API endpoint");
		}

		env.putObject("authzen_api_endpoint_decision", decision.deepCopy());

		JsonElement decisionVal = decision.get("decision");
		if (decisionVal == null) {
			throw error("no decision in decision response");
		}

		logSuccess("Extracted decision from API endpoint response",
			args("decision", decision));

		return env;
	}

}
