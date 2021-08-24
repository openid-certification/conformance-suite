package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilCheckDirectoryApiBase extends AbstractCondition {
	private final String BRAZIL_DIRECTORY_API_BASE = "https://matls-api.sandbox.directory.openbankingbrasil.org.br/";

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String apiBase = getStringFromEnvironment(env, "config", "directory.apibase",
			"Directory API base in test configuration");

		if (!apiBase.equals(BRAZIL_DIRECTORY_API_BASE)) {
			throw error("Testing for Brazil certification must be done using the Brazil directory. If you do not have access to the directory an example client is available in the conformance suite instructions.",
				args("directory_api_base", apiBase,
					"expected", BRAZIL_DIRECTORY_API_BASE));
		}

		logSuccess("Directory API base matches the Brazil directory.", args("actual", apiBase));

		return env;

	}

}
