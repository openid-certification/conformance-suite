package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectAccessDeniedErrorFromAuthorizationEndpointDueToUserRejectingRequest extends AbstractExpectAccessDeniedErrorDueToUserRejectingRequest {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

		checkAccessDeniedError(authorizationEndpointResponse);

		return env;
	}

}
