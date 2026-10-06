package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractAuthzenApiEndpointEvaluationsResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response")
	@PostEnvironment(required = "authzen_evaluations_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonElement bodyElem = env.getElementFromObject("authzen_api_endpoint_response", "body_json");
		if (bodyElem == null) {
			throw error("No json response from AuthZEN API endpoint");
		}
		if (!bodyElem.isJsonObject()) {
			throw error("AuthZEN API evaluations response is not a JSON object",
				args("body_json", bodyElem));
		}
		JsonObject response = bodyElem.getAsJsonObject();
		env.putObject("authzen_evaluations_endpoint_response", response);

		logSuccess("Extracted evaluations from API endpoint response",
			args("response", response));

		return env;
	}

}
