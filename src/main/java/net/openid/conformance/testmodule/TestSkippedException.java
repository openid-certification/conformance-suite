package net.openid.conformance.testmodule;

import java.io.Serial;

public class TestSkippedException extends TestInterruptedException {

	@Serial
	private static final long serialVersionUID = 3681750201628872806L;

	public TestSkippedException(String testId, String msg) {
		super(testId, msg);
	}
}
