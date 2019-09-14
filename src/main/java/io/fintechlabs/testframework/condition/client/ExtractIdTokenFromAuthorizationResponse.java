package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractExtractJWT;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractIdTokenFromAuthorizationResponse extends AbstractExtractJWT {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	@PostEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {

		return extractJWT(env, "authorization_endpoint_response", "id_token", "id_token");

	}

}
