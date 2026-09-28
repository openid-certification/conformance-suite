package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class StartBrazilResourcesPollingTimer extends AbstractCondition {

	@Override
	@PostEnvironment(integers = "brazil_resources_polling_started")
	public Environment evaluate(Environment env) {
		env.putLong("brazil_resources_polling_started", System.nanoTime());
		logSuccess("Started the Resources API polling budget after receiving the CIBA access token");
		return env;
	}
}
