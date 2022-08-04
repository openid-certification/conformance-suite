package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractFAPIBrazilCheckDirectoryDiscoveryUrl extends AbstractCondition {
	abstract String getExpectedUrl();

	abstract String getErrorMessage();

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String discoveryUrl = getStringFromEnvironment(env,
			"config",
			"directory.discoveryUrl",
			"Directory Discovery Endpoint in test configuration"
		);

		String expectedUrl = getExpectedUrl();
		if (!discoveryUrl.equals(expectedUrl)) {
			throw error(getErrorMessage(),
				args("directory_discovery", discoveryUrl,
					"expected", expectedUrl));
		}

		logSuccess("Directory Discovery URL matches the Brazil directory.", args("actual", discoveryUrl));

		return env;

	}

}
