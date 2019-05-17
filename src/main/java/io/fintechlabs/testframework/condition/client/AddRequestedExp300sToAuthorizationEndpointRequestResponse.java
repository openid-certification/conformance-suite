package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;

public class AddRequestedExp300sToAuthorizationEndpointRequestResponse extends AbstractAddRequestedExpToAuthorizationEndpointRequestResponse {

	public AddRequestedExp300sToAuthorizationEndpointRequestResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	protected Integer getExpectedRequestedExpiry() {
		return 300;
	}
}
