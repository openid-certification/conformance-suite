package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.JWKGenerator;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractGenerateClientJWKs extends AbstractCondition {

	public static final int DEFAULT_KEY_SIZE = 2048;

	protected Environment generateClientJWKs(Environment env, JWKGenerator<? extends JWK> generator) {

		JWK key;
		try {
			key = generator.keyUse(KeyUse.SIGNATURE).generate();
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
