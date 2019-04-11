package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import org.springframework.http.HttpStatus;

public class CheckTokenEndpointHttpStatus200 extends AbstractCheckTokenEndpointHttpStatus {

	public CheckTokenEndpointHttpStatus200(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	protected HttpStatus getExpectedHttpStatus() {
		return HttpStatus.OK;
	}

}
