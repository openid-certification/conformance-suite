package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;

public class CreateLongRandomClientNotificationToken extends CreateRandomClientNotificationToken {
	@Override
	protected Integer requestedLength() {
		return 1024;
	}

	public CreateLongRandomClientNotificationToken(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}
}
