package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

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
