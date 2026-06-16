package net.openid.conformance.condition.client;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.PreGeneratedJwks;

public class GenerateRS256ClientJWKs extends AbstractGenerateClientJWKs {

	@Override
	@PostEnvironment(required = {"client_jwks", "client_public_jwks" })
	public Environment evaluate(Environment env) {
		RSAKey key = new RSAKey.Builder(PreGeneratedJwks.nextRsaKey(env, DEFAULT_KEY_SIZE))
			.keyUse(KeyUse.SIGNATURE)
			.algorithm(JWSAlgorithm.RS256)
			.build();
		return publishClientJWKs(env, key);
	}

}
