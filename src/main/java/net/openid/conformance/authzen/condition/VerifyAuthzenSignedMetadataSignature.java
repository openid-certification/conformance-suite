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
 * <p>Metadata carrying {@code signed_metadata} can only be checked against a key, so a
 * missing or empty PDP JWK Set fails rather than silently skipping verification — an
 * unverified signature is not evidence of a valid one. {@link EnsurePDPJwksConfigured}
 * fails first for the same reason in {@link net.openid.conformance.authzen.AbstractAuthzenPDPTest},
 * which is the only caller; the check here keeps the condition safe on its own.
 *
 * <p>For the same reason the condition never reports success without having verified something:
 * it runs only inside {@link ValidateDiscoverySignedMetadata}, behind a gate that fires only when
 * {@code signed_metadata} is present and behind {@link ExtractPDPSignedMetadata}, which has already
 * rejected an absent or non-string value — so every one of those cases is a failure here, not a
 * no-op to log and pass.
 */
public class VerifyAuthzenSignedMetadataSignature extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = {"pdp", "config"})
	public Environment evaluate(Environment env) {
		JsonElement signedMetadataElem = env.getElementFromObject("pdp", "signed_metadata");
		if (signedMetadataElem == null || signedMetadataElem.isJsonNull()) {
			// Nothing reaches this condition unless the metadata carried signed_metadata, so absence here
			// is a broken precondition rather than a PDP that simply did not sign its metadata. Report it
			// as a failure — reporting success would claim a signature had been checked when none exists.
			throw error("Discovery metadata does not contain `signed_metadata`; nothing to verify.");
		}
		if (!signedMetadataElem.isJsonPrimitive() || !signedMetadataElem.getAsJsonPrimitive().isString()) {
			throw error("`signed_metadata` must be a JWT string", args("signed_metadata", signedMetadataElem));
		}
		String signedMetadata = OIDFJSON.getString(signedMetadataElem);

		JsonElement jwksElem = env.getElementFromObject("config", "pdp.jwks");
		if (jwksElem == null || !jwksElem.isJsonObject() || jwksElem.getAsJsonObject().size() == 0) {
			throw error("The PDP's discovery metadata contains `signed_metadata`, but the 'PDP JWK Set' field is "
				+ "empty or missing from the 'AuthZEN' section in the test configuration, so the signature cannot "
				+ "be verified. Supply the PDP's public signing key(s) there.");
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
