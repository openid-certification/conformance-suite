package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class WaitForSuccessfulCibaAuthentication extends AbstractCondition {

	public WaitForSuccessfulCibaAuthentication(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	public Environment evaluate(Environment env) {

		log("Please authenticate and authorize the request");

		return env;

	}

}
