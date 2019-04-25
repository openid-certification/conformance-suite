package io.fintechlabs.testframework.condition.as;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class LogEndTestIfStateIsNotSupplied extends AbstractCondition {

	public LogEndTestIfStateIsNotSupplied(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	public Environment evaluate(Environment env) {

		logSuccess("This test is being skipped as it relies on the client supplying a state value - since none is supplied, we can safely mark this test as successful.");

		return env;
	}
}
