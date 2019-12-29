package net.openid.conformance.testmodule;

public class TestInterruptedException extends RuntimeException {

	private static final long serialVersionUID = 5575434164105800814L;

	private final String testId;

	public TestInterruptedException(String testId, String msg) {
		super(msg);
		this.testId = testId;
	}

	public TestInterruptedException(String testId, Throwable cause) {
		super(cause);
		this.testId = testId;
	}

	public TestInterruptedException(String testId, String msg, Throwable cause) {
		super(msg, cause);
		this.testId = testId;
	}


	public String getTestId() {
		return testId;
	}
}
