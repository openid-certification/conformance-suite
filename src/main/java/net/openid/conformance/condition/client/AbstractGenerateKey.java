package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.condition.AbstractCondition;

public abstract class AbstractGenerateKey extends AbstractCondition {
	public static final int RSA_KEY_SIZE = 2048;

	protected JsonObject createKeyForAlg(String alg) {
		JWK key = createJwkForAlg(alg);
		return JsonParser.parseString(key.toJSONString()).getAsJsonObject();
	}

	protected JWK createJwkForAlg(String alg) {
		JWKGenerator<? extends JWK> generator;
		switch (alg) {
			case "ES256":
				generator = new ECKeyGenerator(Curve.P_256).algorithm(JWSAlgorithm.ES256);
				break;
			case "EdDSA":
				generator = new OctetKeyPairGenerator(Curve.Ed25519).algorithm(JWSAlgorithm.EdDSA);
				break;
			case "PS256":
				generator = new RSAKeyGenerator(RSA_KEY_SIZE).algorithm(JWSAlgorithm.PS256);
				break;
			default:
				throw error("Failed to generate key for alg", args("alg", alg));
		}

		JWK key;
		try {
			key = onConfigure(generator.keyUse(KeyUse.SIGNATURE)).generate();
		} catch (JOSEException e) {
			throw error("Failed to generate key for alg " + alg, e);
		}
		return key;
	}

	protected JWKGenerator<? extends JWK> onConfigure(JWKGenerator<? extends JWK> generator) {
		return generator;
	}
}
