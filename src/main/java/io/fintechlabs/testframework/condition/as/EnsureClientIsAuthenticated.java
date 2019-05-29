package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureClientIsAuthenticated extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public EnsureClientIsAuthenticated(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	// this doesn't use the @PreEnvironment check so that we can have a more specific error message below
	public Environment evaluate(Environment env) {

		if (Strings.isNullOrEmpty(env.getString("client_authentication_success"))) {
			throw error("Client was not authenticated");
		} else {
			logSuccess("Found client authentication, passing", args("client_authentication_success", env.getString("client_authentication_success")));

			return env;
		}

	}

}
