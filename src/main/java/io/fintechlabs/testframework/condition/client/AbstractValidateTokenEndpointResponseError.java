package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.regex.Pattern;

public abstract class AbstractValidateTokenEndpointResponseError extends AbstractCondition {

	protected static final String ERROR_FIELD_PATTERN_VALID = "[\\x20-\\x21\\x23-\\x5B\\x5D-\\x7E]+";

	public AbstractValidateTokenEndpointResponseError(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		if (!env.containsObject("token_endpoint_response")) {
			throw error("Couldn't find token endpoint response");
		}
		validateFieldOfResponseError(env);
		return env;
	}

	protected abstract void validateFieldOfResponseError(Environment env);

	/**
	 * Validate string which has value match with the pattern
	 *
	 * @param str
	 * @param pattern
	 * @return
	 */
	protected boolean isValidFieldFormat(String str, String pattern) {
		Pattern validPattern = Pattern.compile(pattern);
		if (validPattern.matcher(str).matches()) {
			return true;
		}
		return false;
	}
}
