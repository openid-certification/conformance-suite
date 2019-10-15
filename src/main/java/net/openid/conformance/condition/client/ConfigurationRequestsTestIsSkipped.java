package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ConfigurationRequestsTestIsSkipped extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("JSON configuration contains 'skip_test: true'; not running test. System under test cannot be certified.");
	}

}
