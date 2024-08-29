package net.openid.conformance.condition;

import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

public interface Condition {

	/** Setup everything the Condition needs to run; call before evaluate() */
	void setProperties(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements);

	public void execute(Environment env);

	public static enum ConditionResult {
		FAILURE,
		WARNING,
		INFO,
		SUCCESS,
		REVIEW
	}

	/**
	 * Returns a string suitable for tagging this as a "source" in the logs, defaults to the class name
	 */
	default String getMessage() {
		return this.getClass().getSimpleName();
	}

}
