package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddMultipleHintsToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		// add two hints value to authorization endpoint request
		String hintType1 = "login_hint";
		String hintValue1 = "join@example.com";
		authorizationEndpointRequest.addProperty(hintType1, hintValue1);

		String hintType2 = "login_hint_token";
		String hintValue2 = "xxxxxxxxxxxxxxxxxxxx";
		authorizationEndpointRequest.addProperty(hintType2, hintValue2);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added two hints value to authorization endpoint request", args(hintType1, hintValue1, hintType2, hintValue2));

		return env;
	}

}
