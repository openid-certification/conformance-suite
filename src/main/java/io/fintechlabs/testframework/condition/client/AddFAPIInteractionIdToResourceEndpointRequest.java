package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddFAPIInteractionIdToResourceEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "fapi_interaction_id")
	@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		// get the previous headers if they exist
		JsonObject headers = env.getObject("resource_endpoint_request_headers");
		if (headers == null) {
			headers = new JsonObject();
		}

		String interactionId = env.getString("fapi_interaction_id");
		headers.addProperty("x-fapi-interaction-id", interactionId);
		env.putObject("resource_endpoint_request_headers", headers);

		return env;
	}

}
