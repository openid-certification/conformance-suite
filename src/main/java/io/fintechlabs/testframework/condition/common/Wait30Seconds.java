package io.fintechlabs.testframework.condition.common;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.concurrent.TimeUnit;

public class Wait30Seconds extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public Wait30Seconds(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	public Environment evaluate(Environment env) {
		try {
			TimeUnit.SECONDS.sleep(30);
			logSuccess("Paused for 30 seconds");
		} catch (InterruptedException e) {
			throw error("Interrupted while sleeping", e);
		}
		return env;
	}
}
