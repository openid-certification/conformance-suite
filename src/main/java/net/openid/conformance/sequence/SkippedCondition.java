package net.openid.conformance.sequence;

import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.ExecutionContext;
import net.openid.conformance.testmodule.TestExecutionUnit;

public class SkippedCondition implements TestExecutionUnit, DataUtils {

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

	@Override
	public void run(ExecutionContext context) {
		context.getEventLog().log(source, args("msg", message));
	}

}
