package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientPrivateKeysAreDifferent extends AbstractGetSigningKey {

	JWK getSigningKey(Environment env, String envKey) {
		JsonObject jwks = (JsonObject) env.getElementFromObject(envKey, "jwks");
		return getSigningKey(envKey, jwks);
	}

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JWK signingJwkClient1 = getSigningKey(env, "client");
		JWK signingJwkClient2 = getSigningKey(env, "client2");

		if (!signingJwkClient1.getKeyType().equals(signingJwkClient2.getKeyType())) {
			logSuccess("Client signing JWK have different 'kty'",
				args("jwk1", signingJwkClient1, "jwk2", signingJwkClient2));
			return env;
		}

		try {
			String thumb1 = signingJwkClient1.computeThumbprint().toString();
			String thumb2 = signingJwkClient2.computeThumbprint().toString();

			if (thumb1.equals(thumb2)) {
				throw error("Client private keys are the same key. Please use different keys, as otherwise some tests will not work correctly. If you are using the Brazil sandbox directory, you should add different signing keys to each of the client's software statements.",
					args("jwk1", signingJwkClient1, "jwk2", signingJwkClient2));
			}
		} catch (JOSEException e) {
			throw error(e);
		}

		logSuccess("Client signing JWKs have different thumbprints",
			args("jwk1", signingJwkClient1, "jwk2", signingJwkClient2));

		return env;
	}

}
