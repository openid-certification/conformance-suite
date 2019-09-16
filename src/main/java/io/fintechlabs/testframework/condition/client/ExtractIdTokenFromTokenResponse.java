package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractExtractJWT;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractIdTokenFromTokenResponse extends AbstractExtractJWT {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	@PostEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {
		return extractJWT(env, "token_endpoint_response", "id_token", "id_token");
	}

}
