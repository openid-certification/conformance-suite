package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class AddIncorrectNonceToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		String nonce = RandomStringUtils.randomAlphanumeric(10);

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		authorizationEndpointRequest.addProperty("nonce", nonce);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added incorrect nonce parameter to request", authorizationEndpointRequest);

		return env;

	}
}
