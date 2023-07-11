package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetClientIdToResponseUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "response_uri", required = "config")
	public Environment evaluate(Environment env) {
		String responseUri = env.getString("response_uri");

		env.putString("config", "client.client_id", responseUri);

		log("Set client_id to response URI",
			args("client_id", responseUri));

		return env;
	}

}
