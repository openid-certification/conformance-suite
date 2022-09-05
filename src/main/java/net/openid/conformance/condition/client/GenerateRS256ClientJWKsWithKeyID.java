package net.openid.conformance.condition.client;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GenerateRS256ClientJWKsWithKeyID extends AbstractGenerateClientJWKs {

	@Override
	@PostEnvironment(required = {"client_jwks", "client_public_jwks" })
	public Environment evaluate(Environment env) {
		JWKGenerator<RSAKey> generator = new RSAKeyGenerator(DEFAULT_KEY_SIZE)
				.algorithm(JWSAlgorithm.RS256)
				.keyIDFromThumbprint(true);
		return generateClientJWKs(env, generator);
	}

}
