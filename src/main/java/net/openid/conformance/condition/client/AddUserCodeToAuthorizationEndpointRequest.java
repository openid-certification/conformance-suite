package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddUserCodeToAuthorizationEndpointRequest extends AbstractCondition {

	public static final String USER_CODE = "ofbr-ciba-user-code";

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		authorizationEndpointRequest.addProperty("user_code", USER_CODE);
		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added user_code to authorization endpoint request",
			args("user_code", USER_CODE, "authorization_endpoint_request", authorizationEndpointRequest));

		return env;
	}
}
