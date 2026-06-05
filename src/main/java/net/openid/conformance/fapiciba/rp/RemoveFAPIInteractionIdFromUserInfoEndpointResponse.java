package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveFAPIInteractionIdFromUserInfoEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "user_info_endpoint_response_headers")
	@PostEnvironment(required = "user_info_endpoint_response_headers")
	public Environment evaluate(Environment env) {
		JsonObject headers = env.getObject("user_info_endpoint_response_headers");
		headers.remove("x-fapi-interaction-id");

		logSuccess("Removed x-fapi-interaction-id from userinfo endpoint response headers", headers);
		return env;
	}
}
