package io.fintechlabs.testframework.condition.common;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckForKeyIdInClientJWKs extends AbstractCheckForKeyIdinJWKs {

	public CheckForKeyIdInClientJWKs(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "client_jwks")
	public Environment evaluate(Environment env) {

		return checkForKeyIdInJWKs(env, "client_jwks");
	}

}
