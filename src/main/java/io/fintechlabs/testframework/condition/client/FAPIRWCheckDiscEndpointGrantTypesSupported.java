package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.Arrays;

public class FAPIRWCheckDiscEndpointGrantTypesSupported extends ValidateJsonArray {

	private static final String environmentVariable = "grant_types_supported";

	private static final String EXPECTED_VALUE = "authorization_code";
	private static final int minimumMatchesRequired = 1;

	private static final String errorMessageNotEnough = "The server does not support the required grant types.";


	public FAPIRWCheckDiscEndpointGrantTypesSupported(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, Arrays.asList(EXPECTED_VALUE), minimumMatchesRequired, errorMessageNotEnough);

	}
}
