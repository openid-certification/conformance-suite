package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

public abstract class AbstractGenerateClientJWKs extends AbstractCondition {

	public static final int DEFAULT_KEY_SIZE = 2048;

	/** Publish a pre-built client signing JWK as {@code client_jwks} +
	 *  {@code client_public_jwks}. Subclasses that build the JWK themselves
	 *  (e.g. from a pre-generated key pool) can call this directly. */
	protected Environment publishClientJWKs(Environment env, JWK key) {
		JWKSet keys = new JWKSet(key);

		JsonObject jwks = JWKUtil.getPrivateJwksAsJsonObject(keys);
		JsonObject publicJwks = JWKUtil.getPublicJwksAsJsonObject(keys);

		env.putObject("client_jwks", jwks);
		env.putObject("client_public_jwks", publicJwks);

		logSuccess("Generated client JWKs", args("client_jwks", jwks, "public_client_jwks", publicJwks));

		return env;
	}

}
