package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateAtHash extends ValidateHash {

	public ValidateAtHash(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
		super.HashName = "c_hash";
	}

	@Override
	@PreEnvironment(strings = "state", required = "at_hash")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	
}
