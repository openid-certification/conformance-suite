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
		if (!Strings.isNullOrEmpty(errorDescription) && !isValidFieldFormat(errorDescription, ERROR_FIELD_PATTERN_VALID)) {
			throw error("'error_description' field MUST NOT include characters outside the set %x20-21 / %x23-5B / %x5D-7E", args("error_description", errorDescription));
		}
		logSuccess("Token endpoint response error returned expected 'error_description' field", args("error_description", errorDescription));
	}
}
