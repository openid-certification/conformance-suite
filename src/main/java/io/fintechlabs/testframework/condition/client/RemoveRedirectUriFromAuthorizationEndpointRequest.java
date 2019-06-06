package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class RemoveRedirectUriFromAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject req = env.getObject("authorization_endpoint_request");

		req.remove("redirect_uri");

		env.putObject("authorization_endpoint_request", req);

		return env;
	}

}
