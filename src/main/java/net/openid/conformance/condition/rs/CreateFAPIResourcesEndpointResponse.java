package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateFAPIResourcesEndpointResponse extends AbstractOpenBankingApiResponse {

	@Override
	@PreEnvironment(strings = "fapi_interaction_id")
	@PostEnvironment(required = {"resources_endpoint_response", "resources_endpoint_response_headers"})
	public Environment evaluate(Environment env) {

		JsonObject response = JsonParser.parseString(
			"{ \"conformance-test-finished\": \"true\"}").getAsJsonObject();

		String fapiInteractionId = env.getString("fapi_interaction_id");
		if (Strings.isNullOrEmpty(fapiInteractionId)) {
			throw error("Couldn't find FAPI Interaction ID");
		}

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", fapiInteractionId);
		headers.addProperty("content-type", "application/json; charset=UTF-8");

		logSuccess("Created resources response object", args("resources_endpoint_response", response, "resources_endpoint_response_headers", headers));

		env.putObject("resources_endpoint_response", response);
		env.putObject("resources_endpoint_response_headers", headers);

		return env;

	}

}
