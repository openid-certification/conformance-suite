package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExpectAccessDeniedErrorFromAuthorizationEndpointDueToUserRejectingRequest extends AbstractExpectAccessDeniedErrorDueToUserRejectingRequest {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

		checkAccessDeniedError(authorizationEndpointResponse);

		return env;
	}

}
