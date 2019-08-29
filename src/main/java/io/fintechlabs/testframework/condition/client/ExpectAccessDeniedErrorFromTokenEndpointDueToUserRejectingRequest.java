package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExpectAccessDeniedErrorFromTokenEndpointDueToUserRejectingRequest extends AbstractExpectAccessDeniedErrorDueToUserRejectingRequest {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject tokenEndpointResponse = env.getObject("token_endpoint_response");

		checkAccessDeniedError(tokenEndpointResponse);

		return env;
	}
}
