package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddResponseUriAsRedirectUriToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "response_uri", required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		String responseUri = env.getString("response_uri");
		if (Strings.isNullOrEmpty(responseUri)) {
			throw error("Couldn't find response_uri value");
		}

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		authorizationEndpointRequest.addProperty("redirect_uri", responseUri);

		logSuccess("Added response_uri parameter to request", authorizationEndpointRequest);

		return env;

	}
}
