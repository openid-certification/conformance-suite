package net.openid.conformance.condition.client;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.PreGeneratedJwks;

public class GenerateRS256ClientJWKsWithKeyID extends AbstractGenerateClientJWKs {

	@Override
	@PostEnvironment(required = {"client_jwks", "client_public_jwks" })
	public Environment evaluate(Environment env) {
		try {
			RSAKey key = new RSAKey.Builder(PreGeneratedJwks.nextRsaKey(env, DEFAULT_KEY_SIZE))
				.keyUse(KeyUse.SIGNATURE)
				.algorithm(JWSAlgorithm.RS256)
				.keyIDFromThumbprint()
				.build();
			return publishClientJWKs(env, key);
		} catch (JOSEException e) {
			throw error("Failed to build client signing key", e);
		}
	}

}
