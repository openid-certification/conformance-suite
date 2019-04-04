package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.regex.Pattern;

public class ValidateErrorFromTokenEndpointResponseError extends AbstractCondition {

	private static final String ERROR_FIELD_PATTERN_VALID = "[\\x20-\\x21\\x23-\\x5B\\x5D-\\x7E]+";

	public ValidateErrorFromTokenEndpointResponseError(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		String error = env.getString("token_endpoint_response", "error");
		if (Strings.isNullOrEmpty(error)) {
			throw error("'error' field must not null or empty");
		}
		if (!isValidErrorFieldFormat(error)) {
			throw error("'error' field MUST NOT include characters outside the set %x20-21 / %x23-5B / %x5D-7E", args("error", error));
		}
		logSuccess("Token endpoint response error returned valid 'error' field", args("error", error));
		return env;
	}

	private boolean isValidErrorFieldFormat(String str) {
		Pattern validPattern = Pattern.compile(ERROR_FIELD_PATTERN_VALID);
		if (validPattern.matcher(str).matches()) {
			return true;
		}
		return false;
	}
}
