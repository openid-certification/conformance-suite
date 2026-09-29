package net.openid.conformance.testmodule;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import org.slf4j.Logger;

/**
 * Decides whether a condition call is skipped because of the state of the environment.
 */
@FunctionalInterface
public interface ConditionalPreRequisiteValidator {

	/**
	 * @return the result to record if the call is skipped (the skip has already been logged), or null to run it
	 */
	Condition.ConditionResult validatePreRequisite(Logger logger, TestInstanceEventLog eventLog, String testId, Environment env);
}
