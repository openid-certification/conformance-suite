package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddFAPIInteractionIdToPAREndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "fapi_interaction_id", required = "pushed_authorization_request_endpoint_request_headers")
	@PostEnvironment(required = "pushed_authorization_request_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		JsonObject headers = env.getObject("pushed_authorization_request_endpoint_request_headers");

		String interactionId = env.getString("fapi_interaction_id");
		headers.addProperty("x-fapi-interaction-id", interactionId);

		logSuccess("Added x-fapi-interaction-id to pushed authorization endpoint request headers",
			args("pushed_authorization_request_endpoint_request_headers", headers));

		return env;
	}

}
