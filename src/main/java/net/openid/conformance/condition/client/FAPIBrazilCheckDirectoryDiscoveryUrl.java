package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;
import java.util.stream.Collectors;

public class FAPIBrazilCheckDirectoryDiscoveryUrl extends AbstractCondition {
	private final String BRAZIL_SANDBOX_DIRECTORY_DISCOVERY_URL = "https://auth.sandbox.directory.openbankingbrasil.org.br/.well-known/openid-configuration";
	private final String BRAZIL_DIRECTORY_DISCOVERY_URL = "https://auth.directory.openbankingbrasil.org.br/.well-known/openid-configuration";
	private final Set<String> ACCEPTABLE_DIRECTORY_URLS = Set.of(BRAZIL_SANDBOX_DIRECTORY_DISCOVERY_URL, BRAZIL_DIRECTORY_DISCOVERY_URL);

	private final String suggestion = ACCEPTABLE_DIRECTORY_URLS.stream().collect(Collectors.joining(" or "));
	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String discoveryUrl = getStringFromEnvironment(env,
			"config",
			"directory.discoveryUrl",
			"Directory Discovery Endpoint in test configuration"
		);

		if (!ACCEPTABLE_DIRECTORY_URLS.contains(discoveryUrl)) {
			throw error("Testing for Brazil certification must be done using the Brazil directory. If you do not have access to the directory an example client is available in the conformance suite instructions.",
				args("directory_discovery", discoveryUrl,
					"expected", suggestion));
		}

		logSuccess("Directory Discovery URL matches the Brazil directory.", args("actual", discoveryUrl));

		return env;

	}

}
