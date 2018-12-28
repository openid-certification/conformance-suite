package io.fintechlabs.testframework.testmodule;

import io.fintechlabs.testframework.condition.ConditionError;

/**
 * @author jricher
 *
 */
public class TestFailureException extends RuntimeException {

	// this is only used if the "cause" is not a ConditionError
	private String testId = null;

	/**
	 *
	 */
	private static final long serialVersionUID = 7168979969763096442L;

	/**
	 * @param cause
	 */
	public TestFailureException(ConditionError cause) {
		super(cause);
	}

	/**
	 *
	 */
	public TestFailureException(String testId, String msg) {
		super(new RuntimeException(msg));
		this.testId = testId;
	}

	public String getTestId() {
		if (getCause() != null && getCause() instanceof ConditionError) {
			return ((ConditionError) getCause()).getTestId();
		} else {
			return testId;
		}
	}

}
