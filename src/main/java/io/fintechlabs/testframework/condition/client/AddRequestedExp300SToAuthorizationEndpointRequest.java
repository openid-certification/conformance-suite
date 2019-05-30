package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;

public class AddRequestedExp300SToAuthorizationEndpointRequest extends AbstractAddRequestedExpToAuthorizationEndpointRequest {

	public AddRequestedExp300SToAuthorizationEndpointRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	protected Integer getExpectedRequestedExpiry() {
		return 300;
	}
}
