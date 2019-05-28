package io.fintechlabs.testframework.condition.rs;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class RequireBearerAccessToken extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public RequireBearerAccessToken(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(strings = { "incoming_access_token", "access_token" })
	public Environment evaluate(Environment env) {

		String actual = env.getString("incoming_access_token");
		String expected = env.getString("access_token");

		if (!Strings.isNullOrEmpty(actual) && actual.equals(expected)) {
			logSuccess("Found access token in request", args("actual", Strings.nullToEmpty(actual)));
			return env;
		} else {
			throw error("Invalid access token ", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
		}

	}

}
