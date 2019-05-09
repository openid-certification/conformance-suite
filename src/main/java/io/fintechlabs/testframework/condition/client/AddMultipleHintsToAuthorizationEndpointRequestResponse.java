package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddMultipleHintsToAuthorizationEndpointRequestResponse extends AbstractCondition {

	public AddMultipleHintsToAuthorizationEndpointRequestResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

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
