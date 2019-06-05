package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CreateEmptyAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = new JsonObject();

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Created empty authorization endpoint request", authorizationEndpointRequest);

		return env;

	}

}
