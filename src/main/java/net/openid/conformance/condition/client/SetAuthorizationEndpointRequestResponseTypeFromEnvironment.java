package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetAuthorizationEndpointRequestResponseTypeFromEnvironment extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request" }, strings = { "response_type" })
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		String responseType = env.getString("response_type");
		if (Strings.isNullOrEmpty(responseType)) {
			throw error("No response_type found in config");
		}

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		authorizationEndpointRequest.addProperty("response_type", responseType);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added response_type parameter to request", authorizationEndpointRequest);

		return env;
	}

}
