package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public class RedirectQueryTestDisabled extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		/* This condition only exists to log a failure */
		throw error("Tests that use a redirect uri with a query have been disabled by disableRedirectQueryTest in test configuration");
	}

}
