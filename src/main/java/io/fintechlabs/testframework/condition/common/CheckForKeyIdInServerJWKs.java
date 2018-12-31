package io.fintechlabs.testframework.condition.common;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckForKeyIdInServerJWKs extends AbstractCheckForKeyIdinJWKs {

	public CheckForKeyIdInServerJWKs(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "server_jwks")
	public Environment evaluate(Environment env) {

		return checkForKeyIdInJWKs(env, "server_jwks");
	}

}
