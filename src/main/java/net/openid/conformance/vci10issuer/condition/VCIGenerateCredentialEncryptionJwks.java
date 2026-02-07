package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

public class VCIGenerateCredentialEncryptionJwks extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JWKGenerator<? extends JWK> jwkGenerator = new ECKeyGenerator(Curve.P_256);
		jwkGenerator.provider(BouncyCastleProviderSingleton.getInstance());

		jwkGenerator.keyUse(KeyUse.ENCRYPTION);
		jwkGenerator.algorithm(JWEAlgorithm.ECDH_ES);

		JWK jwk;
		try {
			jwk = jwkGenerator.generate();
		} catch (JOSEException e) {
			throw error("Failed to generate credential encryption key", e);
		}

		JWKSet encJwkSet = new JWKSet(jwk);
		JsonObject privateJwksAsJsonObject = JWKUtil.getPrivateJwksAsJsonObject(encJwkSet);
		env.putObject("credential_encryption_jwks", privateJwksAsJsonObject);

		log("Generated credential encryption JWK set", args("credential_encryption_jwks", privateJwksAsJsonObject));

		return env;
	}
}
