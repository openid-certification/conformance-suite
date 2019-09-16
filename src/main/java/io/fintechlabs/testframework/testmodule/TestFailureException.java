package io.fintechlabs.testframework.testmodule;

import io.fintechlabs.testframework.condition.ConditionError;

public class TestFailureException extends TestInterruptedException {

	/**
	 *
	 */
	private static final long serialVersionUID = 7168979969763096443L;

	/**
	 * @param cause
	 */
	public TestFailureException(ConditionError cause) {
		super(cause.getTestId(), cause);
	}

	/**
	 *
	 */
	public TestFailureException(String testId, String msg) {
		super(testId, msg);
	}

	public TestFailureException(String testId, Throwable cause) {
		super(testId, cause);
	}
}
