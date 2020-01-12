package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddPhoneScopeToClientConfiguration extends AbstractAddScopeToClientConfiguration {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		return addScopeToClientConfiguration(env, "phone");
	}

}
