package io.fintechlabs.testframework.condition.as;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ClientContinuedAfterReceivingIdTokenIssuedInPast extends AbstractCondition {

	public ClientContinuedAfterReceivingIdTokenIssuedInPast(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	public Environment evaluate(Environment env) {

		logFailure("Client has incorrectly called token_endpoint after receiving an id_token with an iat value which is a week in the past from the authorization_endpoint.");

		return env;
	}

}
