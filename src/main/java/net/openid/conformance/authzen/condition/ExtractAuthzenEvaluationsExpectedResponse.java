package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractAuthzenEvaluationsExpectedResponse extends AbstractCondition {

	JsonObject expectedResponse;
	public ExtractAuthzenEvaluationsExpectedResponse(String expectedResponseStr) {
		this.expectedResponse = JsonParser.parseString(expectedResponseStr).getAsJsonObject();
	}


	@Override
	@PostEnvironment(required = "authzen_evaluations_endpoint_expected_response")
	public Environment evaluate(Environment env) {
		if(!expectedResponse.has("evaluations")) {
			throw error("The expected evaluations response does not contain an evaluations element", args("expected", expectedResponse));
		} else if(!expectedResponse.get("evaluations").isJsonArray()) {
			throw error("The evaluations element in the expected response is not an array", args("expected", expectedResponse));
		}
		env.putObject("authzen_evaluations_endpoint_expected_response", expectedResponse);
		logSuccess("Extracted evaluations endpoint expected response", args("expected response", expectedResponse));
		return env;
	}

}
