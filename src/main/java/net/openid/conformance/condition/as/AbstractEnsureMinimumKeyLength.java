package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public abstract class AbstractEnsureMinimumKeyLength extends AbstractCondition {
	protected Environment checkKeyLength(Environment env, String jwksKey, int minimumKeyLengthRsa, int minimumKeyLengthEc) {
		JsonObject jwks = env.getObject(jwksKey);
		if (jwks == null) {
			throw error("Couldn't find "+ jwksKey +" in environment");
		}

		JWKSet jwkset;
		try {
			jwkset = JWKSet.parse(jwks.toString());
		} catch (ParseException e) {
			throw error("Failure parsing "+ jwksKey, e);
		}

		for (JWK jwk : jwkset.getKeys()) {
			KeyType keyType = jwk.getKeyType();
			int keyLength = jwk.size();
			int minimumLength;

			if (keyType.equals(KeyType.RSA)) {
				minimumLength = minimumKeyLengthRsa;
			} else if (keyType.equals(KeyType.EC)) {
				minimumLength = minimumKeyLengthEc;
			} else {
				// No requirement for other key types
				continue;
			}

			if (keyLength < minimumLength) {
				throw error("Key found in "+ jwksKey +" has fewer bits (is shorter) than required", args("minimum", minimumLength, "actual", keyLength, "key", jwk.toJSONString()));
			}
		}

		logSuccess("Validated minimum key lengths for "+ jwksKey, args(jwksKey, jwks));

		return env;
	}
}
