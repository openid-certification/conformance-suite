package io.fintechlabs.testframework.condition;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

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
	 * @return a a string suitable for tagging this as a "source" in the logs, defaults to the class name
	 */
	default public String getMessage() {
		return this.getClass().getSimpleName();
	}

}
