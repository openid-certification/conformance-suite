package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

import java.net.URL;

public class ValidateErrorUriFromTokenEndpointResponseError extends AbstractValidateTokenEndpointResponseError {

	private static final String ERROR_URI_FIELD_PATTERN_VALID = "[\\x21\\x23-\\x5B\\x5D-\\x7E]+";

	public ValidateErrorUriFromTokenEndpointResponseError(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	protected void validateFieldOfResponseError(Environment env) {
		String errorUri = env.getString("token_endpoint_response", "error_uri");
		if (!Strings.isNullOrEmpty(errorUri) && !isValidErrorUriFieldFormat(errorUri)) {
			throw error("'error_uri' field MUST conform to the URI-reference syntax and MUST NOT include characters outside the set %x21 / %x23-5B / %x5D-7E", args("error_uri", errorUri));
		}
		logSuccess("Token endpoint response error returned expected 'error_uri' field", args("error_uri", errorUri));
	}

	private boolean isValidErrorUriFieldFormat(String errorUri) {
		return isValidUriSyntax(errorUri) && isValidFieldFormat(errorUri, ERROR_URI_FIELD_PATTERN_VALID);
	}

	private boolean isValidUriSyntax(String errorUri) {
		try {
			new URL(errorUri).toURI();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
