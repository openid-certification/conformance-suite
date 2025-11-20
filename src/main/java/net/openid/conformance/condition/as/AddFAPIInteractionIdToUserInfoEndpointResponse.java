package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddFAPIInteractionIdToUserInfoEndpointResponse extends AbstractCondition {

	@Override
	@PostEnvironment(required = "user_info_endpoint_response_headers")
	public Environment evaluate(Environment env) {

		String fapiInteractionId = env.getString("fapi_interaction_id");
		JsonObject headers = env.getObject("user_info_endpoint_response_headers");

		if (headers == null) {
			headers = new JsonObject();
		}

		if (! Strings.isNullOrEmpty(fapiInteractionId)) {
			headers.addProperty("x-fapi-interaction-id", fapiInteractionId);
			env.putObject("user_info_endpoint_response_headers", headers);
		}

		logSuccess("Added FAPI Interaction ID to userinfo response headers", headers);

		return env;

	}

}
