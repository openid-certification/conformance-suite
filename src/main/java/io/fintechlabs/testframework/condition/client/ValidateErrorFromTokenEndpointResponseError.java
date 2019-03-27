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
			throw error("'error' field must not null or empty");
		}
		if (!isValidFieldFormat(error, ERROR_FIELD_PATTERN_VALID)) {
			throw error("'error' field MUST NOT include characters outside the set %x20-21 / %x23-5B / %x5D-7E", args("error", error));
		}
		logSuccess("Token endpoint response error returned expected 'error' field", args("error", error));
	}
}
