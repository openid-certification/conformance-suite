package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreatePolicyUri extends AbstractCondition {

	private static final String POLICY_URI = "https://openid.net";

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "policy_uri")
	public Environment evaluate(Environment env) {

		env.putString("policy_uri", POLICY_URI);
		log("Generated policy URI", args("policy_uri", POLICY_URI));

		return env;
	}

}
