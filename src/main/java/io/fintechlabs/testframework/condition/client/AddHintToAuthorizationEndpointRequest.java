package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.List;

public class AddHintToAuthorizationEndpointRequest extends AbstractCondition {

	public AddHintToAuthorizationEndpointRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = {"config", "authorization_endpoint_request"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String hintType = env.getString("config", "client.hint_type");
		String hintValue = env.getString("config", "client.hint_value");
		List<String> hintTypeList = ImmutableList.of("login_hint_token", "id_token_hint", "login_hint");

		if (!hintTypeList.contains(hintType)) {
			throw error("the 'hint_type' provided in the configuration must be one of 'login_hint_token', 'id_token_hint' or 'login_hint'");
		}

		if (Strings.isNullOrEmpty(hintValue)) {
			throw error("the 'hint_value' provided in the configuration must not empty");
		}

		authorizationEndpointRequest.addProperty(hintType, hintValue);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added hint to authorization endpoint request", args(hintType, hintValue));

		return env;
	}

}
