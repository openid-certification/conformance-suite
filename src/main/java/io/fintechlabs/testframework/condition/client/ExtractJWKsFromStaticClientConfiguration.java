package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractJWKsFromStaticClientConfiguration extends AbstractExtractJWKsFromClientConfiguration {

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = {"client_jwks", "client_public_jwks" })
	public Environment evaluate(Environment env) {
		// bump the client's internal JWK up to the root

		extractJwks(env, "client");

		return env;
	}

}
