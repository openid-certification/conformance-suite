package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddEmailPhoneAddressProfileScopeToClientConfiguration extends AbstractAddScopeToClientConfiguration {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		return addScopeToClientConfiguration(env, "email phone address profile");
	}

}
