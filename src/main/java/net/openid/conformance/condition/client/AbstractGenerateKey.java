package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.PreGeneratedJwks;

public abstract class AbstractGenerateKey extends AbstractCondition {
	public static final int RSA_KEY_SIZE = 2048;

	protected JsonObject createKeyForAlg(Environment env, String alg) {
		JWK key = createJwkForAlg(env, alg);
		return JsonParser.parseString(key.toJSONString()).getAsJsonObject();
	}

	protected JWK createJwkForAlg(Environment env, String alg) {
		try {
			return switch (alg) {
				case "ES256" -> onConfigureEc(new ECKey.Builder(PreGeneratedJwks.nextEcKey(env, Curve.P_256))
					.keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.ES256)).build();
				case "EdDSA" -> onConfigureOkp(new OctetKeyPair.Builder(PreGeneratedJwks.nextOkpKey(env, Curve.Ed25519))
					.keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.EdDSA)).build();
				case "PS256" -> onConfigureRsa(new RSAKey.Builder(PreGeneratedJwks.nextRsaKey(env, RSA_KEY_SIZE))
					.keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.PS256)).build();
				default -> throw error("Failed to generate key for alg", args("alg", alg));
			};
		} catch (JOSEException e) {
			throw error("Failed to build key for alg " + alg, e);
		}
	}

	/** Extension point: applied to the EC builder before {@code build()} (e.g.
	 *  to add {@code keyIDFromThumbprint(true)}). Default is a no-op. */
	protected ECKey.Builder onConfigureEc(ECKey.Builder b) throws JOSEException {
		return b;
	}

	/** Extension point for OKP builders; see {@link #onConfigureEc}. */
	protected OctetKeyPair.Builder onConfigureOkp(OctetKeyPair.Builder b) throws JOSEException {
		return b;
	}

	/** Extension point for RSA builders; see {@link #onConfigureEc}. */
	protected RSAKey.Builder onConfigureRsa(RSAKey.Builder b) throws JOSEException {
		return b;
	}
}
