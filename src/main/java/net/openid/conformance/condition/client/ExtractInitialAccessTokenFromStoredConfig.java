package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractInitialAccessTokenFromStoredConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "original_client_config")
	public Environment evaluate(Environment env) {
		env.removeNativeValue("initial_access_token");

		// pull out any initial access token and put it in the root environment for easy access (if there is one)
		String initialAccessToken = env.getString("original_client_config", "initial_access_token");
		if (!Strings.isNullOrEmpty(initialAccessToken)) {
			env.putString("initial_access_token", initialAccessToken);
		}
		log("Extracted initial access_token from stored client configuration.",
			args("initial_access_token", Strings.emptyToNull(initialAccessToken)));

		return env;
	}

}
