package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class EnsureEmptyImplicitHash extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public EnsureEmptyImplicitHash(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(strings = "implicit_hash")
	public Environment evaluate(Environment env) {

		String implicitHash = env.getString("implicit_hash");

		if (implicitHash == null) {
			throw error("Implicit hash was null when it should have been an empty string");
		}

		if (implicitHash.isEmpty()) {
			logSuccess("Implicit hash was empty string, no implicit parameters detected");
			return env;
		} else {
			throw error("Implicit hash was not empty", args("implicit_hash", implicitHash));
		}

	}

}
