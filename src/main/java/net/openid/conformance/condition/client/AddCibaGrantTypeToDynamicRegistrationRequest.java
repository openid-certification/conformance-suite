package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddCibaGrantTypeToDynamicRegistrationRequest extends AbstractAddGrantTypeToDynamicRegistrationRequest {

	@Override
	@PreEnvironment(required = {"dynamic_registration_request"})
	public Environment evaluate(Environment env) {

		addGrantType(env, "urn:openid:params:grant-type:ciba");

		return env;
	}

}
