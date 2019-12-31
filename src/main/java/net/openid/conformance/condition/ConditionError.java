package net.openid.conformance.condition;

/**
 * Record a failure from a condition
 *
 * This should only be created from the error() methods in AbstractCondition.java, which will also add a log entry.
 * Conditions should use the error() methods to create a ConditionError. TestModules should create
 * TestFailureExceptions that contain something other than ConditionError as their cause - for example:
 *
 * throw new TestFailureException(getId(), "some error");
 *
 * see https://gitlab.com/openid/conformance-suite/issues/443
 */
public class ConditionError extends RuntimeException {

	private static final long serialVersionUID = 6331346678545936565L;

	private String testId;

	private boolean isPreOrPostError;

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
	 * @param testId
	 * @param message
	 * @param isPreOrPostError Contravening a pre/post requirement is always a failure, even if failure of the condition would otherwise be a warning
	 */
	public ConditionError(String testId, String message, boolean isPreOrPostError) {
		super(message);
		this.testId = testId;
		this.isPreOrPostError = isPreOrPostError;
	}

	/**
	 * @param testId
	 * @param message
	 * @param isPreOrPostError Contravening a pre/post requirement is always a failure, even if failure of the condition would otherwise be a warning
	 * @param cause
	 */
	public ConditionError(String testId, String message, boolean isPreOrPostError, Throwable cause) {
		super(message, cause);
		this.testId = testId;
		this.isPreOrPostError = isPreOrPostError;
	}

	/**
	 * @return the testId
	 */
	public String getTestId() {
		return testId;
	}

	public boolean isPreOrPostError() {
		return isPreOrPostError;
	}
}
