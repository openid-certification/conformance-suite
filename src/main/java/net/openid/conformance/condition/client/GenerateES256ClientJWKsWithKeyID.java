package net.openid.conformance.condition.client;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.PreGeneratedJwks;

public class GenerateES256ClientJWKsWithKeyID extends AbstractGenerateClientJWKs {

	@Override
	@PostEnvironment(required = {"client_jwks", "client_public_jwks" })
	public Environment evaluate(Environment env) {
		try {
			ECKey key = new ECKey.Builder(PreGeneratedJwks.nextEcKey(env, Curve.P_256))
				.keyUse(KeyUse.SIGNATURE)
				.algorithm(JWSAlgorithm.ES256)
				.keyIDFromThumbprint()
				.build();
			return publishClientJWKs(env, key);
		} catch (JOSEException e) {
			throw error("Failed to build client signing key", e);
		}
	}

}
