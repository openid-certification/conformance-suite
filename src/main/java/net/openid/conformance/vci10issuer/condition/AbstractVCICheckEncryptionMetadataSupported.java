package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Shared logic for validating the credential_request_encryption and credential_response_encryption
 * blocks of credential issuer metadata. Both blocks are OPTIONAL; when present they share the
 * required fields enc_values_supported, encryption_required, and the optional zip_values_supported.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-12.2.4">OID4VCI Section 12.2.4 - Credential Issuer Metadata</a>
 */
public abstract class AbstractVCICheckEncryptionMetadataSupported extends AbstractCondition {

	protected abstract String getMetadataKey();

	/** Subclasses validate fields specific to request- or response-side encryption metadata. */
	protected abstract void checkDirectionSpecificFields(JsonObject encryptionMetadata);

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		String key = getMetadataKey();
		String path = "credential_issuer_metadata." + key;

		JsonElement el = env.getElementFromObject("vci", path);
		if (el == null) {
			logSuccess(key + " is not present in credential issuer metadata");
			return env;
		}

		if (!el.isJsonObject()) {
			throw error("credential_issuer_metadata." + key + " is present but is not a JSON object",
				args(key, el));
		}
		JsonObject encryptionMetadata = el.getAsJsonObject();

		checkDirectionSpecificFields(encryptionMetadata);
		requireNonEmptyArray(encryptionMetadata, "enc_values_supported");
		requireBoolean(encryptionMetadata, "encryption_required");
		requireOptionalNonEmptyArray(encryptionMetadata, "zip_values_supported");

		logSuccess("Checked " + key + " metadata", args(key, encryptionMetadata));

		return env;
	}

	protected void requireNonEmptyArray(JsonObject parent, String field) {
		JsonElement el = parent.get(field);
		if (el == null || !el.isJsonArray()) {
			throw error("Required " + getMetadataKey() + "." + field
				+ " is missing or not a JSON array", args(field, el));
		}
		if (el.getAsJsonArray().isEmpty()) {
			throw error(getMetadataKey() + "." + field + " must not be empty", args(field, el));
		}
	}

	protected void requireBoolean(JsonObject parent, String field) {
		JsonElement el = parent.get(field);
		if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
			throw error("Required " + getMetadataKey() + "." + field
				+ " is missing or not a JSON boolean", args(field, el));
		}
	}

	protected void requireOptionalNonEmptyArray(JsonObject parent, String field) {
		JsonElement el = parent.get(field);
		if (el == null) {
			return;
		}
		if (!el.isJsonArray() || el.getAsJsonArray().isEmpty()) {
			throw error(getMetadataKey() + "." + field
				+ " must not be an empty JSON array", args(field, el));
		}
	}
}
