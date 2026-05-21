package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractAuthzenDecisionExpectedResponse extends AbstractCondition {

	JsonObject expectedResponse;
	public ExtractAuthzenDecisionExpectedResponse(String expectedResponseStr) {
		this.expectedResponse = JsonParser.parseString(expectedResponseStr).getAsJsonObject();
	}

	@Override
	@PostEnvironment(required = "authzen_decision_endpoint_expected_response")
	public Environment evaluate(Environment env) {
		if(!expectedResponse.has("decision")) {
			throw error("The expected decision response does not contain a decision element", args("expected", expectedResponse));
		}
		env.putObject("authzen_decision_endpoint_expected_response", expectedResponse);
		logSuccess("Extracted decision endpoint expected response", args("expected response", expectedResponse));
		return env;
	}

}
