package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class SetAuthorizationEndpointRequestResponseTypeFromConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "config" })
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		String responseType = env.getString("config", "response_type");
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
