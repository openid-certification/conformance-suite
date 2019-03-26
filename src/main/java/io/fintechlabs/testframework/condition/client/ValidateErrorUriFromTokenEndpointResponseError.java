package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateErrorUriFromTokenEndpointResponseError extends AbstractValidateTokenEndpointResponseError {

	public ValidateErrorUriFromTokenEndpointResponseError(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	protected void validateFieldOfResponseError(Environment env) {
		String errorUri = env.getString("token_endpoint_response", "error_uri");
		if (!Strings.isNullOrEmpty(errorUri) && !isValidFieldFormat(errorUri)) {
			throw error("'error_uri' field has unexpected value");
		}
		logSuccess("Token endpoint response error returned expected 'error_uri' field", args("error_uri", errorUri));
	}
}
