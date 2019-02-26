package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddHintToAuthorizationEndpointRequestResponse extends AbstractCondition {

	public AddHintToAuthorizationEndpointRequestResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		// FIXME read BOTH these values from configuration
		final String hintType = "login_hint"; // one of "login_hint_token", "id_token_hint" or "login_hint"
		final String hintValue = "john@example.com";
		authorizationEndpointRequest.addProperty(hintType, hintValue);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added hint to authorization endpoint request", args(hintType, hintValue));

		return env;
	}

}
