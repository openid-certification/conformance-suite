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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Adds additional "decoy" JWKs with the same kid but different algorithms to the JWKSet to ensure that
 * clients perform proper JWK lookups which also considers `alg` and `kty`.
 */
public class AugmentRealJwksWithDecoys extends AbstractCondition {

	static final Set<String> FAPI_JWK_ALGORITHMS = Set.of("EdDSA", "PS256", "ES256");

	@Override
	@PreEnvironment(required = {"server_jwks"})
	@PostEnvironment(required = {"server_public_jwks_decoy"})
	public Environment evaluate(Environment env) {

		JsonObject serverJwksJsonObject = env.getObject("server_jwks");
		JWKSet publicJWKSet;
		try {
			// extract the real public JWKSet
			publicJWKSet = JWKUtil.parseJWKSet(serverJwksJsonObject.toString()).toPublicJWKSet();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}


		// reference public key
		JWK publicKey;

		if (publicJWKSet == null || publicJWKSet.getKeys().isEmpty()) {
			logFailure("No public JWK found in server_jwks. Cannot generate decoy JWKs.");
			return env;
		}

		// try to find the public JWK used for signing
		List<JWK> publicKeysForSigning = publicJWKSet.getKeys().stream().filter(jwk -> jwk.getKeyUse() == KeyUse.SIGNATURE).toList();
		if (!publicKeysForSigning.isEmpty()) {
			// we found public keys with use=sig
			if (publicKeysForSigning.size() == 1) {
				// found a single public key with use=sig, generating decoy keys with the same kid for the missing algorithms
				publicKey = publicKeysForSigning.get(0);
			} else {
				// more than one public keys found
				// check if the JWKS already contains Keys with the same kid for the desired "decoy" algorithms.
				Map<String, Set<String>> kidsWithMissingFapiAlgorithms = findKeyIdsWithMissingFapiAlgorithms(publicKeysForSigning);

				if (!kidsWithMissingFapiAlgorithms.isEmpty()) {
					// Shall we just generate a decoy for the missing algorithm here?
					logFailure("Existing server_jwks contains multiple keys for use=sig, but desired JWK variant is missing",
						Map.of("jwks_ids_with_missing_fapi_algorithms", kidsWithMissingFapiAlgorithms));
					return env;
				}

				// all desired fapi algorithms are provided by the JWKS
				log("Existing server_jwks already contains the desired JWK variants. Skipping decoy JWKS key generation.");
				return env;
			}
		} else {
			log("Found no public keys with use=sig in server_jwks");
			// try to use another JWK if present
			if (publicJWKSet.getKeys().size() == 1) {
				// TODO check shall we fail here?
				log("Found a single JWK in server_jwks without use=sig, using it for now.");
				publicKey = publicJWKSet.getKeys().get(0);
			} else {
				// found multiple public keys but we don't know which one to use.
				logFailure("Found ambiguous JWKs in server_jwks without use=sig.");
				return env;
			}
		}

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

		List<JWK> keysWithDecoys = new ArrayList<>();
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
		String decoyJwksUri = baseUrl + "/jwks_decoy";
		env.putString("server", "jwks_uri", decoyJwksUri);

		logSuccess("Updated jwks_uri with decoy keys", Map.of("jwks_uri", decoyJwksUri, "publicJwksWithDecoys", publicJwksWithDecoys));

		return env;
	}

	/**
	 * Some providers already contain multiple JWKs with use=sig, the same kid but different algorithms. Find those kid's where a FAPI algorithm is missing.
	 * @param publicKeysForSigning
	 * @return
	 */
	private static Map<String, Set<String>> findKeyIdsWithMissingFapiAlgorithms(List<JWK> publicKeysForSigning) {
		Map<String, Set<String>> idToAlgs = new HashMap<>();
		for (var key : publicKeysForSigning) {
			idToAlgs.compute(key.getKeyID(), (k, v) -> Objects.requireNonNullElseGet(v, HashSet::new)).add(key.getAlgorithm().getName());
		}
		Map<String, Set<String>> kidsWithMissingFapiAlgorithms = new HashMap<>();
		for (var entry : idToAlgs.entrySet()) {
			boolean keysForFapiAlgorithmsPresent = entry.getValue().containsAll(FAPI_JWK_ALGORITHMS);
			if (!keysForFapiAlgorithmsPresent) {
				Set<String> missing = new HashSet<>(FAPI_JWK_ALGORITHMS);
				missing.removeAll(entry.getValue());
				kidsWithMissingFapiAlgorithms.put(entry.getKey(), missing);
			}
		}
		return kidsWithMissingFapiAlgorithms;
	}
}
