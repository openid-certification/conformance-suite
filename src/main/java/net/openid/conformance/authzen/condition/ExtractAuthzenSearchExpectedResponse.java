package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractAuthzenSearchExpectedResponse extends AbstractCondition {

	JsonObject expectedResponse;
	public ExtractAuthzenSearchExpectedResponse(String expectedResponseStr) {
		this.expectedResponse = JsonParser.parseString(expectedResponseStr).getAsJsonObject();
	}


	@Override
	@PostEnvironment(required = "authzen_search_endpoint_expected_response")
	public Environment evaluate(Environment env) {
		if(!expectedResponse.has("results")) {
			throw error("The expected search response does not contain a results element", args("expected", expectedResponse));
		} else if(!expectedResponse.get("results").isJsonArray()) {
			throw error("The results element in the expected response is not an array", args("expected", expectedResponse));
		}
		env.putObject("authzen_search_endpoint_expected_response", expectedResponse);
		logSuccess("Extracted search endpoint expected response", args("expected response", expectedResponse));
		return env;
	}

}
