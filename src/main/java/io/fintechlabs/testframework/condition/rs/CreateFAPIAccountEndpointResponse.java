package io.fintechlabs.testframework.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CreateFAPIAccountEndpointResponse extends AbstractOpenBankingApiResponse {

	public CreateFAPIAccountEndpointResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String[] requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(strings = "fapi_interaction_id")
	@PostEnvironment(required = {"accounts_endpoint_response", "accounts_endpoint_response_headers"})
	public Environment evaluate(Environment env) {

		JsonObject response = new JsonParser().parse(
			"{ \"conformance-test-finished\": \"true\"}").getAsJsonObject();

		String fapiInteractionId = env.getString("fapi_interaction_id");
		if (Strings.isNullOrEmpty(fapiInteractionId)) {
			throw error("Couldn't find FAPI Interaction ID");
		}

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", fapiInteractionId);
		headers.addProperty("content-type", "application/json; charset=UTF-8");

		logSuccess("Created account response object", args("accounts_endpoint_response", response, "accounts_endpoint_response_headers", headers));

		env.putObject("accounts_endpoint_response", response);
		env.putObject("accounts_endpoint_response_headers", headers);

		return env;

	}

}
