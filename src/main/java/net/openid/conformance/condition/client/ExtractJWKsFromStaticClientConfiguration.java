package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

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
