package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import org.springframework.http.HttpStatus;

public class CheckBackchannelAuthenticationEndpointHttpStatus401 extends AbstractCheckBackchannelAuthenticationEndpointHttpStatus {

	public CheckBackchannelAuthenticationEndpointHttpStatus401(String testId, TestInstanceEventLog log, Condition.ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	protected HttpStatus getExpectedHttpStatus() {
		return HttpStatus.UNAUTHORIZED;
	}
}
