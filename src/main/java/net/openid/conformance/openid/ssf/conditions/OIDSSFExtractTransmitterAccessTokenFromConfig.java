package net.openid.conformance.openid.ssf.conditions;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFExtractTransmitterAccessTokenFromConfig extends AbstractCondition {

	@PreEnvironment(required = {"config"})
	@Override
	public Environment evaluate(Environment env) {

		// TODO add auth variant (dynamic / static)
		String accessTokenFromConfig = env.getString("config", "ssf.transmitter.access_token");

		if (accessTokenFromConfig != null) {
			log("Obtained transmitter access token from config");
			env.putString("access_token", "value", accessTokenFromConfig);
			env.putString("access_token", "type", "Bearer");
		}

		logSuccess("Added transmitter access token");

		return env;
	}
}
