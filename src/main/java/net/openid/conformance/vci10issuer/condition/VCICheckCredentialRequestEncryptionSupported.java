package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

/**
 * Detects whether the credential issuer advertises credential request encryption support.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-12.2.4">OID4VCI Section 12.2.4 - Credential Issuer Metadata</a>
 */
public class VCICheckCredentialRequestEncryptionSupported extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonElement requestEncEl = env.getElementFromObject("vci",
			"credential_issuer_metadata.credential_request_encryption");

		// credential_request_encryption is OPTIONAL. If absent there is nothing to validate —
		// callers decide what to do based on the presence of the JSON element itself.
		if (requestEncEl == null) {
			logSuccess("credential_request_encryption is not present in credential issuer metadata");
			return env;
		}

		if (!requestEncEl.isJsonObject()) {
			throw error("credential_issuer_metadata.credential_request_encryption is present but is not a JSON object",
				args("credential_request_encryption", requestEncEl));
		}

		JsonElement jwksEl = env.getElementFromObject("vci",
			"credential_issuer_metadata.credential_request_encryption.jwks");
		if (jwksEl == null || !jwksEl.isJsonObject()) {
			throw error("Required credential_issuer_metadata.credential_request_encryption.jwks is missing or not a JSON object",
				args("jwks", jwksEl));
		}
		validateJwksKids(jwksEl.getAsJsonObject());

		JsonElement encValuesEl = env.getElementFromObject("vci",
			"credential_issuer_metadata.credential_request_encryption.enc_values_supported");
		if (encValuesEl == null || !encValuesEl.isJsonArray()) {
			throw error("Required credential_issuer_metadata.credential_request_encryption.enc_values_supported is missing or not a JSON array",
				args("enc_values_supported", encValuesEl));
		}
		validateEncValuesSupported(encValuesEl.getAsJsonArray());

		JsonElement encryptionRequiredEl = env.getElementFromObject("vci",
			"credential_issuer_metadata.credential_request_encryption.encryption_required");
		validateEncryptionRequired(encryptionRequiredEl);

		JsonElement zipValuesEl = env.getElementFromObject("vci",
			"credential_issuer_metadata.credential_request_encryption.zip_values_supported");
		if (zipValuesEl != null) {
			validateZipValuesSupported(zipValuesEl);
		}

		logSuccess("Checked credential_request_encryption metadata",
			args("jwks", jwksEl,
				"enc_values_supported", encValuesEl));

		return env;
	}

	/**
	 * Per OID4VCI 1.0 Final §12.2.4: "Each JWK in the set MUST have a kid (Key ID) parameter
	 * that uniquely identifies the key." Throws a test failure if any JWK is missing a kid or
	 * if kids are not distinct within the JWKS.
	 */
	protected void validateJwksKids(JsonObject jwks) {
		JsonElement keysEl = jwks.get("keys");
		if (keysEl == null || !keysEl.isJsonArray()) {
			throw error("credential_issuer_metadata.credential_request_encryption.jwks.keys is missing or not an array; "
				+ "jwks MUST be a JSON Web Key Set with a 'keys' array",
				args("jwks", jwks));
		}

		JsonArray keys = keysEl.getAsJsonArray();
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
			if (kidEl == null || !kidEl.isJsonPrimitive() || OIDFJSON.getString(kidEl).isEmpty()) {
				throw error("credential_issuer_metadata.credential_request_encryption.jwks.keys[" + i + "] "
					+ "is missing a 'kid'",
					args("key", key, "jwks", jwks));
			}
			String kid = OIDFJSON.getString(kidEl);
			if (!seenKids.add(kid)) {
				throw error("credential_issuer_metadata.credential_request_encryption.jwks contains a duplicate kid '" + kid + "'; "
					+ "each kid MUST uniquely identify a key",
					args("duplicate_kid", kid, "jwks", jwks));
			}
		}
	}

	/**
	 * Per OID4VCI 1.0 Final §12.2.4: "enc_values_supported: REQUIRED. A non-empty array
	 * containing a list of the JWE encryption algorithms …". Throws a test failure if the
	 * array is empty.
	 */
	protected void validateEncValuesSupported(JsonArray encValuesSupported) {
		if (encValuesSupported.isEmpty()) {
			throw error("credential_issuer_metadata.credential_request_encryption.enc_values_supported must not be empty",
				args("enc_values_supported", encValuesSupported));
		}
	}

	/**
	 * Per OID4VCI 1.0 Final §12.2.4: "encryption_required: REQUIRED. Boolean value specifying
	 * whether the Credential Issuer requires the additional encryption on top of TLS for the
	 * Credential Requests." Throws a test failure if the field is missing or not a JSON boolean.
	 */
	protected void validateEncryptionRequired(JsonElement encryptionRequiredEl) {
		if (encryptionRequiredEl == null
			|| !encryptionRequiredEl.isJsonPrimitive()
			|| !encryptionRequiredEl.getAsJsonPrimitive().isBoolean()) {
			throw error("Required credential_issuer_metadata.credential_request_encryption.encryption_required is missing or not a JSON boolean",
				args("encryption_required", encryptionRequiredEl));
		}
	}

	/**
	 * Per OID4VCI 1.0 Final §12.2.4: "zip_values_supported: OPTIONAL. A non-empty array
	 * containing a list of the JWE compression algorithms …". If the field is present it
	 * MUST be a non-empty JSON array.
	 */
	protected void validateZipValuesSupported(JsonElement zipValuesSupported) {
		if (!zipValuesSupported.isJsonArray() || zipValuesSupported.getAsJsonArray().isEmpty()) {
			throw error("credential_issuer_metadata.credential_request_encryption.zip_values_supported must not be an empty JSON array",
				args("zip_values_supported", zipValuesSupported));
		}
	}
}
