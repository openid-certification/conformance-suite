package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWEUtil;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates the credential_request_encryption block of credential issuer metadata, if present.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-12.2.4">OID4VCI Section 12.2.4 - Credential Issuer Metadata</a>
 */
public class VCICheckCredentialRequestEncryptionSupported extends AbstractVCICheckEncryptionMetadataSupported {

	@Override
	protected String getMetadataKey() {
		return "credential_request_encryption";
	}

	@Override
	protected void checkDirectionSpecificFields(JsonObject encryptionMetadata) {
		JsonElement jwksEl = encryptionMetadata.get("jwks");
		if (jwksEl == null || !jwksEl.isJsonObject()) {
			throw error("Required credential_issuer_metadata.credential_request_encryption.jwks is missing or not a JSON object",
				args("jwks", jwksEl));
		}
		validateJwks(jwksEl.getAsJsonObject());
	}

	// Per OID4VCI 1.0 Final §12.2.4 each JWK MUST have a kid that uniquely identifies the key.
	// Section 10 also requires the selected request-encryption key to advertise an asymmetric JWE alg.
	protected void validateJwks(JsonObject jwks) {
		JsonElement keysEl = jwks.get("keys");
		if (keysEl == null || !keysEl.isJsonArray()) {
			throw error("credential_issuer_metadata.credential_request_encryption.jwks.keys is missing or not an array; "
				+ "jwks MUST be a JSON Web Key Set with a 'keys' array",
				args("jwks", jwks));
		}

		JsonArray keys = keysEl.getAsJsonArray();
		if (keys.isEmpty()) {
			throw error("credential_issuer_metadata.credential_request_encryption.jwks.keys must not be empty",
				args("jwks", jwks));
		}

		Set<String> seenKids = new HashSet<>();
		for (int i = 0; i < keys.size(); i++) {
			JsonElement keyEl = keys.get(i);
			if (!keyEl.isJsonObject()) {
				throw error("credential_issuer_metadata.credential_request_encryption.jwks.keys[" + i + "] "
					+ "is not a JSON object",
					args("key", keyEl, "jwks", jwks));
			}
			JsonObject key = keyEl.getAsJsonObject();
			JsonElement kidEl = key.get("kid");
			if (kidEl == null || !kidEl.isJsonPrimitive()) {
				throw error("credential_issuer_metadata.credential_request_encryption.jwks.keys[" + i + "] "
					+ "is missing a 'kid'",
					args("key", key, "jwks", jwks));
			}
			String kid = OIDFJSON.getString(kidEl);
			if (kid.isBlank()) {
				throw error("credential_issuer_metadata.credential_request_encryption.jwks.keys[" + i + "] "
					+ "has an empty 'kid'",
					args("key", key, "jwks", jwks));
			}
			if (!seenKids.add(kid)) {
				throw error("credential_issuer_metadata.credential_request_encryption.jwks contains a duplicate kid '" + kid + "'; "
					+ "each kid MUST uniquely identify a key",
					args("duplicate_kid", kid, "jwks", jwks));
			}
		}

		ensureAtLeastOneUsableEncryptionKey(jwks);
	}

	protected void ensureAtLeastOneUsableEncryptionKey(JsonObject jwks) {
		JWKSet jwkSet;
		try {
			jwkSet = JWKUtil.parseJWKSet(jwks.toString());
		} catch (ParseException e) {
			throw error("Failed to parse credential_issuer_metadata.credential_request_encryption.jwks", e,
				args("jwks", jwks));
		}

		boolean hasUsableKey = jwkSet.getKeys().stream()
			.anyMatch(this::isUsableForCredentialRequestEncryption);
		if (!hasUsableKey) {
			throw error("credential_issuer_metadata.credential_request_encryption.jwks does not contain a usable "
				+ "credential request encryption key; expected at least one key with use=enc (or absent) and "
				+ "an asymmetric JWE alg per OID4VCI 1.0 Section 10",
				args("jwks", jwks));
		}
	}

	protected boolean isUsableForCredentialRequestEncryption(JWK candidate) {
		if (candidate.getAlgorithm() == null) {
			return false;
		}
		if (candidate.getKeyUse() != null && !KeyUse.ENCRYPTION.equals(candidate.getKeyUse())) {
			return false;
		}
		return JWEUtil.isAsymmetricJWEAlgorithm(candidate.getAlgorithm().getName());
	}
}
