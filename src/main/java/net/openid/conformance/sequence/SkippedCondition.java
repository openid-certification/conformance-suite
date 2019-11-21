package net.openid.conformance.sequence;

import net.openid.conformance.testmodule.TestExecutionUnit;

public class SkippedCondition implements TestExecutionUnit {

	private String source;
	private String message;

	public SkippedCondition(String source, String message) {
		this.source = source;
		this.message = message;
	}

	public String getSource() {
		return source;
	}

	public String getMessage() {
		return message;
	}

}
