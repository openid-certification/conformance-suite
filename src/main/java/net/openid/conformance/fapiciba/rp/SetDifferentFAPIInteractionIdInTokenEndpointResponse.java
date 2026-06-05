package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class SetDifferentFAPIInteractionIdInTokenEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response_headers", strings = "fapi_interaction_id")
	@PostEnvironment(required = "token_endpoint_response_headers")
	public Environment evaluate(Environment env) {
		String requestInteractionId = env.getString("fapi_interaction_id");
		String responseInteractionId;
		do {
			responseInteractionId = UUID.randomUUID().toString();
		} while (responseInteractionId.equalsIgnoreCase(requestInteractionId));

		JsonObject headers = env.getObject("token_endpoint_response_headers");
		headers.addProperty("x-fapi-interaction-id", responseInteractionId);

		logSuccess("Set a different x-fapi-interaction-id in token endpoint response headers",
			args("request_interaction_id", requestInteractionId,
				"response_interaction_id", responseInteractionId));
		return env;
	}
}
