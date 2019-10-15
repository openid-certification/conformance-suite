package net.openid.conformance.condition.as;

import java.text.ParseException;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientSigningKeySize extends AbstractCondition {

	private static final int MINIMUM_KEY_LENGTH_RSA = 2048;

	private static final int MINIMUM_KEY_LENGTH_EC = 160;

	@Override
	@PreEnvironment(required = "client_jwks")
	public Environment evaluate(Environment env) {

		JsonObject jwks = env.getObject("client_jwks");
		if (jwks == null) {
			throw error("Couldn't find JWKs in environment");
		}

		JWKSet jwkset;
		try {
			jwkset = JWKSet.parse(jwks.toString());
		} catch (ParseException e) {
			throw error("Failure parsing JWK Set", e);
		}

		for (JWK jwk : jwkset.getKeys()) {
			KeyType keyType = jwk.getKeyType();
			int keyLength = jwk.size();
			int minimumLength;

			if (keyType.equals(KeyType.RSA)) {
				minimumLength = MINIMUM_KEY_LENGTH_RSA;
			} else if (keyType.equals(KeyType.EC)) {
				minimumLength = MINIMUM_KEY_LENGTH_EC;
			} else {
				// No requirement for other key types
				continue;
			}

			if (keyLength < minimumLength) {
				throw error("Key length too short", args("minimum", minimumLength, "actual", keyLength, "key", jwk));
			}
		}

		logSuccess("Validated minimum key lengths", args("server_jwks", jwks));

		return env;
	}

}
