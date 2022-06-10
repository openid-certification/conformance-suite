package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;
import java.util.stream.Collectors;

public class FAPIBrazilCheckDirectoryApiBase extends AbstractCondition {
	private final String BRAZIL_SANDBOX_DIRECTORY_API_BASE = "https://matls-api.sandbox.directory.openbankingbrasil.org.br/";
	private final String BRAZIL_DIRECTORY_API_BASE = "https://matls-api.directory.openbankingbrasil.org.br/";
	private final Set<String> ACCEPTABLE_API_URLS = Set.of(BRAZIL_SANDBOX_DIRECTORY_API_BASE, BRAZIL_DIRECTORY_API_BASE);
	private final String suggestion = ACCEPTABLE_API_URLS.stream().collect(Collectors.joining(" or "));

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String apiBase = getStringFromEnvironment(env, "config", "directory.apibase",
			"Directory API base in test configuration");

		if (!ACCEPTABLE_API_URLS.contains(apiBase)) {
			throw error("Testing for Brazil certification must be done using the Brazil directory. If you do not have access to the directory an example client is available in the conformance suite instructions.",
				args("directory_api_base", apiBase,
					"expected", suggestion));
		}

		logSuccess("Directory API base matches the Brazil directory.", args("actual", apiBase));

		return env;

	}

}
