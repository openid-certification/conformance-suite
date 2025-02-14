package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetClientIdToWebOrigin extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "origin", required = "config")
	public Environment evaluate(Environment env) {
		String origin = env.getString("origin");

		env.putString("config", "client.client_id", origin);

		log("Set client_id to web origin URI",
			args("client_id", origin));

		return env;
	}

}
