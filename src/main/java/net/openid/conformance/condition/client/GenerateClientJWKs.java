package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GenerateClientJWKs extends AbstractCondition {

	private static final JWSAlgorithm DEFAULT_ALGORITHM = JWSAlgorithm.RS256;
	private static final int DEFAULT_KEY_SIZE = 2048;

	@Override
	@PostEnvironment(required = {"client_jwks", "client_public_jwks" })
	public Environment evaluate(Environment env) {

		RSAKey key;
		try {
			key = new RSAKeyGenerator(DEFAULT_KEY_SIZE)
					.algorithm(DEFAULT_ALGORITHM)
					.keyUse(KeyUse.SIGNATURE)
					// no key ID
					.generate();
		} catch (JOSEException e) {
			throw error("Failed to generate RSA key", e);
		}

		JWKSet keys = new JWKSet(key);

		JsonObject jwks = new JsonParser().parse(keys.toJSONObject(false).toJSONString()).getAsJsonObject();
		JsonObject publicJwks = new JsonParser().parse(keys.toJSONObject(true).toJSONString()).getAsJsonObject();

		env.putObject("client_jwks", jwks);
		env.putObject("client_public_jwks", publicJwks);

		logSuccess("Generated client JWKs", args("client_jwks", jwks, "public_client_jwks", publicJwks));

		return env;
	}

}
