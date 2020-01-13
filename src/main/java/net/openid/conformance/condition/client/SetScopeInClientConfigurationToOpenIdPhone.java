package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetScopeInClientConfigurationToOpenIdPhone extends AbstractSetScopeInClientConfiguration {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		return setScopeInClientConfiguration(env, "openid phone");
	}

}
