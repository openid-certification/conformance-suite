package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class RedirectQueryTestDisabled extends AbstractCondition {

	public RedirectQueryTestDisabled(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	public Environment evaluate(Environment env) {
		/* This condition only exists to log a failure */
		throw error("Tests that use a redirect uri with a query have been disabled by disableRedirectQueryTest in test configuration");
	}

}
