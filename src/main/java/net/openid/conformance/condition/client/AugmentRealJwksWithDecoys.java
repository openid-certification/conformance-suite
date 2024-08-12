package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adds additional "decoy" JWKs with the same kid but different algorithms to the JWKSet to ensure that
 * clients perform proper JWK lookups which also considers `alg` and `kty`.
 */
public class AugmentRealJwksWithDecoys extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server_public_jwks", "base_url", "server"})
	@PostEnvironment(required = {"server_public_jwks_decoy"})
	public Environment evaluate(Environment env) {

		// extract the real public JWKSet
		JsonObject realServerPublicKeysJsonObject = env.getObject("server_public_jwks");

		JWKSet publicJWKSet;
		try {
			publicJWKSet = JWKUtil.parseJWKSet(realServerPublicKeysJsonObject.toString());
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		List<JWK> keysWithDecoys = new ArrayList<>();

		// pick public key used for signing, assume there is only one
		JWK publicKey = publicJWKSet.getKeys().stream().filter(jwk -> jwk.getKeyUse() == KeyUse.SIGNATURE).toList().get(0);

		// determine ID of public key with use=sig
		String keyID = publicKey.getKeyID();
		Algorithm alg = publicKey.getAlgorithm();

		var keyGenerator = new AbstractGenerateKey() {
			@Override
			public Environment evaluate(Environment env) {
				return null;
			}

			@Override
			protected JWKGenerator<? extends JWK> onConfigure(JWKGenerator<? extends JWK> generator) {
				// use same kid for all keys
				generator.keyID(keyID);
				return generator;
			}
		};

		// generate decoy keys
		switch (alg.getName()) {
			case "ES256":
				// generate decoys with same kid for the "other" algorithms
				keysWithDecoys.add(keyGenerator.createJwkForAlg("EdDSA")); // decoy jwk
				keysWithDecoys.add(publicKey); // real jwk
				keysWithDecoys.add(keyGenerator.createJwkForAlg("PS256")); // decoy jwk
				break;
			case "EdDSA":
				keysWithDecoys.add(keyGenerator.createJwkForAlg("ES256"));
				keysWithDecoys.add(publicKey); // real jwk
				keysWithDecoys.add(keyGenerator.createJwkForAlg("PS256"));
				break;
			case "PS256":
				keysWithDecoys.add(keyGenerator.createJwkForAlg("EdDSA"));
				keysWithDecoys.add(publicKey); // real jwk
				keysWithDecoys.add(keyGenerator.createJwkForAlg("ES256"));
				break;
			default:
				throw error("Invalid FAPI alg detected in JWK", Map.of("alg", alg.getName()));
		}

		// generate a new JWKS set with the real key and the decoys
		JWKSet jwkSet = new JWKSet(keysWithDecoys);
		JsonObject publicJwksWithDecoys = JWKUtil.getPublicJwksAsJsonObject(jwkSet);

		// expose the new decoy keys
		env.putObject("server_public_jwks_decoy", publicJwksWithDecoys);

		// update jwks URI in server object with jwks_decoy endpoint variant
		// provided by net.openid.conformance.fapi2spid2.AbstractFAPI2SPID2ClientTest.handleClientRequestForPath
		String baseUrl = env.getString("base_url");
		String decoyJwksUri = baseUrl + "jwks_decoy";
		env.putString("server", "jwks_uri", decoyJwksUri);

		logSuccess("Updated jwks_uri with decoy keys", Map.of("jwks_uri", decoyJwksUri, "publicJwksWithDecoys", publicJwksWithDecoys));

		return env;
	}
}
