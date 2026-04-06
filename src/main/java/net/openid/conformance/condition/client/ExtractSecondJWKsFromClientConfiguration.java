package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractSecondJWKsFromClientConfiguration extends AbstractExtractJWKsFromClientConfiguration {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "client2_jwks")
	public Environment evaluate(Environment env) {
		JsonElement jwks = env.getElementFromObject("config", "client2.jwks");
		if (jwks == null) {
			throw error("'Second Client JWKS' field is missing from the test configuration");
		}
		extractJwks(env, jwks, "client2_jwks", "client2_public_jwks");

		return env;
	}

}
