package io.fintechlabs.testframework.condition.client;


import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import org.springframework.http.HttpStatus;

public class CheckTokenEndpointHttpStatus401 extends AbstractCheckTokenEndpointHttpStatus {

	public CheckTokenEndpointHttpStatus401(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	protected HttpStatus getExpectedHttpStatus() {
		return HttpStatus.UNAUTHORIZED;
	}
}
