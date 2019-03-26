package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateErrorDescriptionFromTokenEndpointResponseError extends AbstractValidateTokenEndpointResponseError {

	public ValidateErrorDescriptionFromTokenEndpointResponseError(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	protected void validateFieldOfResponseError(Environment env) {
		String errorDescription = env.getString("token_endpoint_response", "error_description");
		if (!Strings.isNullOrEmpty(errorDescription) && !isValidFieldFormat(errorDescription)) {
			throw error("'error_description' field has unexpected value");
		}
		logSuccess("Token endpoint response error returned expected 'error_description' field", args("error_description", errorDescription));
	}
}
