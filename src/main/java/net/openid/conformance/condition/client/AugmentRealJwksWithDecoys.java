package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
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
 *
 * <p>The signing key is selected from {@code server_jwks}. One decoy is inserted immediately before and one immediately after that
 * key in {@code server_public_jwks} (derived from {@code server_jwks} if not yet present); all other published keys are kept.
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

		// decoys exercise selection among keys sharing a kid; without a kid there is nothing to share
		if (publicKey.getKeyID() == null) {
			logSuccess("Skipping JWKS decoy generation for server_jwks signing key without a kid.");
			return env;
		}

		// edited as JSON rather than via JWKSet so already-published keys Nimbus cannot parse are kept
		JsonObject existingPublicJwks = env.getObject("server_public_jwks");
		if (existingPublicJwks == null) {
			existingPublicJwks = JWKUtil.getPublicJwksAsJsonObject(publicJWKSet);
		}
		if (!JWKUtil.hasKeysArray(existingPublicJwks)) {
			throw error("server_public_jwks does not contain a 'keys' array", args("server_public_jwks", existingPublicJwks));
		}
		JsonArray existingKeys = existingPublicJwks.getAsJsonArray("keys");

		int realKeyIndex = indexOfKey(existingKeys, publicKey);
		if (realKeyIndex < 0) {
			throw error("server_public_jwks does not contain the server signing key from server_jwks",
				args("server_public_jwks", existingPublicJwks, "kid", publicKey.getKeyID(), "alg", publicKey.getAlgorithm().getName()));
		}

		List<JWK> decoys = generateDecoyPublicKeys(env, publicKey);
		JsonArray keysWithDecoys = new JsonArray();
		for (int i = 0; i < existingKeys.size(); i++) {
			if (i == realKeyIndex) {
				keysWithDecoys.add(toJsonObject(decoys.get(0)));
			}
			keysWithDecoys.add(existingKeys.get(i).deepCopy());
			if (i == realKeyIndex) {
				keysWithDecoys.add(toJsonObject(decoys.get(1)));
			}
		}
		JsonObject publicJwksWithDecoys = existingPublicJwks.deepCopy();
		publicJwksWithDecoys.add("keys", keysWithDecoys);

		env.putObject("server_public_jwks", publicJwksWithDecoys);

		logSuccess("Augmented JWKS with decoy keys.", Map.of("existingJwks", existingPublicJwks, "jwksWithDecoys", publicJwksWithDecoys));

		return env;
	}

	private static int indexOfKey(JsonArray keys, JWK key) {
		String kid = key.getKeyID();
		String alg = key.getAlgorithm().getName();
		for (int i = 0; i < keys.size(); i++) {
			if (!keys.get(i).isJsonObject()) {
				continue;
			}
			JsonObject candidate = keys.get(i).getAsJsonObject();
			if (isStringEqual(candidate.get("kid"), kid) && isStringEqual(candidate.get("alg"), alg)) {
				return i;
			}
		}
		return -1;
	}

	private static boolean isStringEqual(JsonElement element, String value) {
		return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
			&& OIDFJSON.getString(element).equals(value);
	}

	private static JsonObject toJsonObject(JWK jwk) {
		return JsonParser.parseString(jwk.toJSONString()).getAsJsonObject();
	}

	/**
	 * Generates two decoy public keys with the same kid as the given key but different algorithms. The first decoy is published
	 * before the real key and the second after it.
	 */
	List<JWK> generateDecoyPublicKeys(Environment env, JWK publicKey) {

		String keyID = publicKey.getKeyID();
		Algorithm alg = publicKey.getAlgorithm();

		var keyGenerator = new AbstractGenerateKey() {
			@Override
			public Environment evaluate(Environment env) {
				return null;
			}

			// use same kid for all keys

			@Override
			protected ECKey.Builder onConfigureEc(ECKey.Builder b) throws JOSEException {
				return b.keyID(keyID);
			}

			@Override
			protected OctetKeyPair.Builder onConfigureOkp(OctetKeyPair.Builder b) throws JOSEException {
				return b.keyID(keyID);
			}

			@Override
			protected RSAKey.Builder onConfigureRsa(RSAKey.Builder b) throws JOSEException {
				return b.keyID(keyID);
			}
		};

		// generate decoys with same kid for the "other" algorithms
		List<String> decoyAlgs = switch (alg.getName()) {
			case "ES256" -> List.of("EdDSA", "PS256");
			// EdDSA and Ed25519 are the same curve/kty, so they're treated as the same "family" here
			case "EdDSA", "Ed25519" -> List.of("ES256", "PS256");
			case "PS256" -> List.of("EdDSA", "ES256");
			default -> throw error("Invalid FAPI alg detected in JWK", Map.of("alg", alg.getName()));
		};

		List<JWK> decoys = new ArrayList<>();
		for (String decoyAlg : decoyAlgs) {
			decoys.add(keyGenerator.createJwkForAlg(env, decoyAlg).toPublicJWK());
		}
		return decoys;
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
			// EdDSA and Ed25519 are the same curve/kty, so treat them as the same "family" for this check
			String algName = key.getAlgorithm().getName();
			if ("Ed25519".equals(algName)) {
				algName = "EdDSA";
			}
			idToAlgs.compute(key.getKeyID(), (k, v) -> Objects.requireNonNullElseGet(v, HashSet::new)).add(algName);
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
