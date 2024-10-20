package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import net.openid.conformance.condition.AbstractCondition;
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
 * Adds additional "decoy" JWKs with the same kid but different algorithms to the {@code server_public_jwks} JWKSet to ensure that
 * clients perform proper JWK lookups by {@code kid} including {@code alg} and {@code kty}.
 */
public class AugmentRealJwksWithDecoys extends AbstractCondition {

	static final Set<String> FAPI_JWK_ALGORITHMS = Set.of("EdDSA", "PS256", "ES256");

	@Override
	@PreEnvironment(required = {"server_jwks"})
	public Environment evaluate(Environment env) {

		JsonObject serverJwksJsonObject = env.getObject("server_jwks");
		JWKSet publicJWKSet;
		try {
			// extract the real public JWKSet
			publicJWKSet = JWKUtil.parseJWKSet(serverJwksJsonObject.toString()).toPublicJWKSet();
		} catch (Exception e) {
			throw error("Failed to parse server_jwks", e);
		}

		if (publicJWKSet.getKeys().isEmpty()) {
			logSuccess("Skipping JWKS decoy generation for server_jwks with missing public keys.");
			return env;
		}

		// try to find the public JWK used for signing
		List<JWK> publicKeysForSigning = publicJWKSet.getKeys().stream().filter(jwk -> KeyUse.SIGNATURE.equals(jwk.getKeyUse())).toList();
		if (publicKeysForSigning.isEmpty()) {

			logSuccess("Skipping JWKS decoy generation for server_jwks with missing public keys of use=sig.");
			return env;
		}

		// we found public keys with use=sig
		if (publicKeysForSigning.size() != 1) {
			// we found more than one public key
			// check if the JWKS already contains keys with the same kid for all desired "decoy" algorithms.
			Map<String, Set<String>> kidsWithMissingFapiAlgorithms = findKeyIdsWithMissingFapiAlgorithms(publicKeysForSigning);

			if (!kidsWithMissingFapiAlgorithms.isEmpty()) {
				// Shall we just generate a decoy for the missing algorithm here?
				logFailure("Existing server_jwks contains multiple keys for use=sig, but desired JWK variant is missing", Map.of("jwks_ids_with_missing_fapi_algorithms", kidsWithMissingFapiAlgorithms));
				return env;
			}

			// all desired fapi algorithms are provided by the JWKS
			logSuccess("Existing server_jwks already contains JWKs with the desired algorithm variants. Skipping generation of additional decoy JWKs.");
			return env;
		}

		// we found a single public key with use=sig, generating decoy keys with the same kid for the missing algorithms
		// reference public key
		JWK publicKey = publicKeysForSigning.get(0);
		if (publicKey.getAlgorithm() == null) {
			logFailure("Public JWK with use=sig is missing alg information.", Map.of("kid", publicKey.getKeyID(), "alg", publicKey.getAlgorithm()));
			return env;
		}

		JsonObject publicJwksWithDecoys = generateDecoyPublicKeysAroundGivenPublicKey(publicKey);

		// augment the current server_public_jwks with additional decoy keys
		env.putObject("server_public_jwks", publicJwksWithDecoys);

		logSuccess("Augmented JWKS with decoy keys.", Map.of("existingJwks", publicJWKSet, "jwksWithDecoys", publicJwksWithDecoys));

		return env;
	}

	JsonObject generateDecoyPublicKeysAroundGivenPublicKey(JWK publicKey) {

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

		// generate a new JWKS with the real key and the decoys
		return JWKUtil.getPublicJwksAsJsonObject(new JWKSet(keysWithDecoys));
	}

	/**
	 * Some providers already contain multiple JWKs with use=sig, the same kid but different algorithms. Find those kid's where a FAPI algorithm is missing.
	 *
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
