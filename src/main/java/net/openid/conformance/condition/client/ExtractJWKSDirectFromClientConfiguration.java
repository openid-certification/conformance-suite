package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

// Extracts the JWKS from the 'client' element within the configuration
public class ExtractJWKSDirectFromClientConfiguration extends AbstractExtractJWKsFromClientConfiguration {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = {"client_jwks", "client_public_jwks" })
	public Environment evaluate(Environment env) {
		// bump the client's internal JWK up to the root
		JsonElement jwks = env.getElementFromObject("config", "client.jwks");
		extractJwks(env, jwks);

		return env;
	}

}
