package io.fintechlabs.testframework.testmodule;

public class TestSkippedException extends TestInterruptedException {

	private static final long serialVersionUID = 3681750201628872806L;

	public TestSkippedException(String testId, String msg) {
		super(testId, msg);
	}
}
