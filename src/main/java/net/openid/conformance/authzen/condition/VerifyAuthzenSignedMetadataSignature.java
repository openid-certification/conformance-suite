package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Verify the signature of the discovery metadata {@code signed_metadata} JWT
 * against the PDP public key(s) supplied in the test configuration
 * ({@code pdp.jwks}, the "PDP JWK Set" field).
 *
 * <p>Certification profile (https://github.com/openid/authzen/issues/433 §6.5):
 * "If {@code signed_metadata} is present, it is a valid JWT string with a
 * verifiable signature ...". AuthZEN metadata defines no {@code jwks_uri}, so the
 * trusted verification key is provided out of band via the test configuration.
 *
 * <p>When the metadata carries no {@code signed_metadata}, or no PDP JWK Set is
 * configured, signature verification is skipped (and logged); when both are
 * present, a signature that does not verify against the configured key(s) fails.
 */
public class VerifyAuthzenSignedMetadataSignature extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = {"pdp", "config"})
	public Environment evaluate(Environment env) {
		JsonElement signedMetadataElem = env.getElementFromObject("pdp", "signed_metadata");
		if (signedMetadataElem == null || signedMetadataElem.isJsonNull()) {
			logSuccess("Discovery metadata does not contain `signed_metadata`; no signature to verify");
			return env;
		}
		if (!signedMetadataElem.isJsonPrimitive() || !signedMetadataElem.getAsJsonPrimitive().isString()) {
			throw error("`signed_metadata` must be a JWT string", args("signed_metadata", signedMetadataElem));
		}
		String signedMetadata = OIDFJSON.getString(signedMetadataElem);

		JsonElement jwksElem = env.getElementFromObject("config", "pdp.jwks");
		if (jwksElem == null || !jwksElem.isJsonObject() || jwksElem.getAsJsonObject().size() == 0) {
			log("`signed_metadata` is present but no 'PDP JWK Set' is configured in the test configuration; "
				+ "skipping signature verification. Provide the PDP's public signing key(s) in the 'PDP JWK Set' "
				+ "field to verify the signed_metadata signature.");
			return env;
		}

		JsonObject jwks = normalizeToJwkSet(jwksElem.getAsJsonObject());
		verifyJwsSignature(signedMetadata, jwks, "signed_metadata", false, "PDP JWK Set");
		return env;
	}

	/**
	 * Accept either a JWK Set ({@code {"keys": [...]}}) or a bare single JWK,
	 * wrapping the latter so the shared verifier always receives a JWK Set.
	 */
	private JsonObject normalizeToJwkSet(JsonObject configured) {
		if (configured.has("keys")) {
			return configured;
		}
		JsonObject jwks = new JsonObject();
		JsonArray keys = new JsonArray();
		keys.add(configured);
		jwks.add("keys", keys);
		return jwks;
	}
}
