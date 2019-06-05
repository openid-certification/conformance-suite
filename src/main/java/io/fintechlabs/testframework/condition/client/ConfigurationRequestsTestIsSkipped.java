package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public class ConfigurationRequestsTestIsSkipped extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("JSON configuration contains 'skip_test: true'; not running test. System under test cannot be certified.");
	}

}
