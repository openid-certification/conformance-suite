package net.openid.conformance.condition.client;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GenerateES256ClientJWKsWithKeyID extends AbstractGenerateClientJWKs {

	@Override
	@PostEnvironment(required = {"client_jwks", "client_public_jwks" })
	public Environment evaluate(Environment env) {
		JWKGenerator<? extends JWK> generator = new ECKeyGenerator(Curve.P_256)
				.algorithm(JWSAlgorithm.ES256)
				.keyIDFromThumbprint(true);
		return generateClientJWKs(env, generator);
	}

}
