package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureAuthzenEvaluationsResponseElemNumbersMatchRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_api_endpoint_request", "authzen_evaluations_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		JsonObject response = env.getObject("authzen_evaluations_endpoint_response");
		int expectedNum = request.get("evaluations").getAsJsonArray().size();
		int actualNum = response.get("evaluations").getAsJsonArray().size();
		if( expectedNum != actualNum) {
			throw error("The number of evaluations elements in the response does not match the request",
				args("expected", expectedNum, "actual", actualNum));
		}
		logSuccess("The number of evaluations elements in the response match the request", args("actual", actualNum));
		return env;
	}

}
