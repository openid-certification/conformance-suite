package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetClientIdToRedirectUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "redirect_uri", required = "config")
	public Environment evaluate(Environment env) {
		String redirectUri = env.getString("redirect_uri");

		env.putString("config", "client.client_id", redirectUri);

		log("Set client_id to redirect URI",
			args("client_id", redirectUri));

		return env;
	}

}
