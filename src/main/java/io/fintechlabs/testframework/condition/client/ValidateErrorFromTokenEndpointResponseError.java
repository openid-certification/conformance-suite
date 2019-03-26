package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateErrorFromTokenEndpointResponseError extends AbstractValidateTokenEndpointResponseError {

	public ValidateErrorFromTokenEndpointResponseError(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	protected void validateFieldOfResponseError(Environment env) {
		String error = env.getString("token_endpoint_response", "error");
		if (Strings.isNullOrEmpty(error)) {
			throw error("Expected 'error' field not null or empty");
		}
		if (!isValidFieldFormat(error)) {
			throw error("'error' field has unexpected value");
		}
		logSuccess("Token endpoint response error returned expected 'error' field", args("error", error));
	}
}
