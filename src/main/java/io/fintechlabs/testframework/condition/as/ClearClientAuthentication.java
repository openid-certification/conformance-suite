package io.fintechlabs.testframework.condition.as;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ClearClientAuthentication extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ClearClientAuthentication(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {

		// remove the client auth from the environment
		env.removeNativeValue("client_authentication_success");
		env.removeObject("client_authentication");

		return env;

	}

}
