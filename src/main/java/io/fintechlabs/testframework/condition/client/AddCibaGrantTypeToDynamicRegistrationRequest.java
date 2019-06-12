package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddCibaGrantTypeToDynamicRegistrationRequest extends AbstractAddGrantTypeToDynamicRegistrationRequest {

	@Override
	@PreEnvironment(required = {"dynamic_registration_request"})
	public Environment evaluate(Environment env) {

		addGrantType(env, "urn:openid:params:grant-type:ciba");

		return env;
	}

}
