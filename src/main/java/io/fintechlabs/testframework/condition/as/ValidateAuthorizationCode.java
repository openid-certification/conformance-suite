package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateAuthorizationCode extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public ValidateAuthorizationCode(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(strings = "authorization_code", required = "token_endpoint_request")
	public Environment evaluate(Environment env) {

		String expected = env.getString("authorization_code");
		String actual = env.getString("token_endpoint_request", "params.code");

		if (Strings.isNullOrEmpty(expected)) {
			throw error("Couldn't find authorization code to compare");
		}

		if (expected.equals(actual)) {
			logSuccess("Found authorization code", args("authorization_code", actual));
			return env;
		} else {
			throw error("Didn't find matching authorization code", args("expected", expected, "actual", actual));
		}
	}

}
