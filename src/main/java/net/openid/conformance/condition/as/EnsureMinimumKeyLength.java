package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class EnsureMinimumKeyLength extends AbstractCondition {

	private static final int MINIMUM_KEY_LENGTH_RSA = 2048;

	private static final int MINIMUM_KEY_LENGTH_EC = 160;

	private static final String JWKS_KEY = "server_jwks";

	@Override
	@PreEnvironment(required = JWKS_KEY)
	public Environment evaluate(Environment env) {

		JsonObject jwks = env.getObject(JWKS_KEY);
		if (jwks == null) {
			throw error("Couldn't find "+JWKS_KEY+" in environment");
		}

		JWKSet jwkset;
		try {
			jwkset = JWKSet.parse(jwks.toString());
		} catch (ParseException e) {
			throw error("Failure parsing "+JWKS_KEY, e);
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
				throw error("Key found in "+JWKS_KEY+" has fewer bits (is shorter) than required", args("minimum", minimumLength, "actual", keyLength, "key", jwk));
			}
		}

		logSuccess("Validated minimum key lengths for "+JWKS_KEY, args(JWKS_KEY, jwks));

		return env;
	}

}
