package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;

public class CheckErrorFromTokenEndpointResponseErrorInvalidGrant extends AbstractCheckErrorFromTokenEndpointResponseError {

	public CheckErrorFromTokenEndpointResponseErrorInvalidGrant(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	protected String getExpectedError() {
		return "invalid_grant";
	}
}
