package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;

public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidClient  extends AbstractCheckErrorFromBackchannelAuthenticationEndpointError {

	public CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidClient(String testId, TestInstanceEventLog log, Condition.ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	protected String getExpectedError() {
		return "invalid_client";
	}
}
