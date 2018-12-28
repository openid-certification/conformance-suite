package io.fintechlabs.testframework.condition;

/**
 * @author jricher
 *
 */
public class ConditionError extends RuntimeException {

	private static final long serialVersionUID = 6331346678545936565L;

	private String testId;

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public ConditionError(String testId, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.testId = testId;
	}

	/**
	 * @param message
	 * @param cause
	 */
	public ConditionError(String testId, String message, Throwable cause) {
		super(message, cause);
		this.testId = testId;
	}

	/**
	 * @param message
	 */
	public ConditionError(String testId, String message) {
		super(message);
		this.testId = testId;
	}

	/**
	 * @return the testId
	 */
	public String getTestId() {
		return testId;
	}

}
