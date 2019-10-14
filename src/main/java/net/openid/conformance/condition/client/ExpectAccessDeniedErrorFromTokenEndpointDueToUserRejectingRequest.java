package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectAccessDeniedErrorFromTokenEndpointDueToUserRejectingRequest extends AbstractExpectAccessDeniedErrorDueToUserRejectingRequest {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject tokenEndpointResponse = env.getObject("token_endpoint_response");

		checkAccessDeniedError(tokenEndpointResponse);

		return env;
	}
}
