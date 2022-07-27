package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractFAPIBrazilCheckDirectoryApiBase extends AbstractCondition {
	protected abstract String getErrorMessage();

	abstract String getExpectedUrl();

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String apiBase = getStringFromEnvironment(env, "config", "directory.apibase",
			"Directory API base in test configuration");

		String BRAZIL_DIRECTORY_API_BASE = getExpectedUrl();
		if (!apiBase.equals(BRAZIL_DIRECTORY_API_BASE)) {
			throw error(getErrorMessage(),
				args("directory_api_base", apiBase,
					"expected", BRAZIL_DIRECTORY_API_BASE));
		}

		logSuccess("Directory API base matches the Brazil directory.", args("actual", apiBase));

		return env;

	}
}
