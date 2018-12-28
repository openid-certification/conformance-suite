package io.fintechlabs.testframework.condition;

import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public interface Condition {

	/**
	 * Tests if the condition holds true. Reads from the given environment and returns a potentially modified environment.
	 *
	 * Throws ConditionError when condition isn't met.
	 *
	 * Decorate with @PreEnvironment to ensure objects or strings are in the environment before evaluation.
	 * Decorate with @PostEnvironment to ensure objects or strings are in the environment after evaluation.
	 */
	Environment evaluate(Environment env);

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
