package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.Arrays;

public class FAPIRWCheckDiscEndpointResponseTypesSupported extends ValidateJsonArray {

	private static final String environmentVariable = "response_types_supported";

	private static final String[] SET_VALUES = new String[]{"code id_token"};

	private static final String errorMessageNotEnough = "No matching value from server";

	public FAPIRWCheckDiscEndpointResponseTypesSupported(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), 1,
			errorMessageNotEnough);

	}
}
