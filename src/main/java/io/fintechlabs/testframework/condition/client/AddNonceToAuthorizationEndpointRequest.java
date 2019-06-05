package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddNonceToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "nonce", required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		String nonce = env.getString("nonce");
		if (Strings.isNullOrEmpty(nonce)) {
			throw error("Couldn't find nonce value");
		}

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		authorizationEndpointRequest.addProperty("nonce", nonce);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added nonce parameter to request", authorizationEndpointRequest);

		return env;

	}

}
