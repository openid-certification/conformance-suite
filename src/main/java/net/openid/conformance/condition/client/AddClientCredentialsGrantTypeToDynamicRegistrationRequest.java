package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddClientCredentialsGrantTypeToDynamicRegistrationRequest extends AbstractAddGrantTypeToDynamicRegistrationRequest {

	@Override
	@PreEnvironment(required = {"dynamic_registration_request"})
	public Environment evaluate(Environment env) {

		addGrantType(env, "client_credentials");

		return env;
	}

}
