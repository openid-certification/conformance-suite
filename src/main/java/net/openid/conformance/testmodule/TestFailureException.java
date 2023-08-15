package net.openid.conformance.testmodule;

import net.openid.conformance.condition.ConditionError;

/**
 * General exception for anything that has gone wrong in a TestModule
 *
 * All exceptions thrown from a TestModule should eventually end up wrapped in a TestFailureException, so that the
 * code catching the exception is able to identify the testId that the exception should be recorded against.
 *
 * A 'cause' of a ConditionError is a special case. ConditionErrors should only be created by AbstractCondition's
 * error() methods - these add log messages meaning the rest of the suite can/should avoid creating further log
 * messages for TestFailureExceptions with causes of ConditionError.
 *
 * This exception is ultmatively handled in AbstractTestModule.handleException
 */
public class TestFailureException extends TestInterruptedException {

	/**
	 *
	 */
	private static final long serialVersionUID = 7168979969763096443L;
	private String errorDescription;

	public String getError() {
		return super.getMessage();
	}

	public String getErrorDescription() {
		return errorDescription;
	}


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

	public TestFailureException(String testId, String msg, Throwable cause) {
		super(testId, msg, cause);
	}

	/**
	 * Constructor overload to allow creating OAuth2 style error responses,
	 * ref https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.2.1.
	 *
	 * @param testId
	 * @param error
	 * @param errorDescription
	 */
	public TestFailureException(String testId, String error, String errorDescription) {
		super(testId, error);
		this.errorDescription = errorDescription;
	}
}
