package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateTosUri extends AbstractCondition {

	private static final String TOS_URI = "https://openid.net";

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "tos_uri")
	public Environment evaluate(Environment env) {

		env.putString("tos_uri", TOS_URI);
		log("Generated TOS URI", args("tos_uri", TOS_URI));

		return env;
	}

}
