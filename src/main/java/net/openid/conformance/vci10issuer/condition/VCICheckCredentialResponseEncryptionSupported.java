package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Detects whether the credential issuer advertises credential response encryption support.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-12.2.4">OID4VCI Section 12.2.4 - Credential Issuer Metadata</a>
 */
public class VCICheckCredentialResponseEncryptionSupported extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonElement responseEncEl = env.getElementFromObject("vci",
			"credential_issuer_metadata.credential_response_encryption");

		// credential_response_encryption is OPTIONAL. If absent there is nothing to validate —
		// callers decide what to do based on the presence of the JSON element itself.
		if (responseEncEl == null) {
			logSuccess("credential_response_encryption is not present in credential issuer metadata");
			return env;
		}

		if (!responseEncEl.isJsonObject()) {
			throw error("credential_issuer_metadata.credential_response_encryption is present but is not a JSON object",
				args("credential_response_encryption", responseEncEl));
		}

		JsonElement algValuesEl = env.getElementFromObject("vci",
			"credential_issuer_metadata.credential_response_encryption.alg_values_supported");
		if (algValuesEl == null || !algValuesEl.isJsonArray()) {
			throw error("Required credential_issuer_metadata.credential_response_encryption.alg_values_supported is missing or not a JSON array",
				args("alg_values_supported", algValuesEl));
		}
		validateAlgValuesSupported(algValuesEl.getAsJsonArray());

		JsonElement encValuesEl = env.getElementFromObject("vci",
			"credential_issuer_metadata.credential_response_encryption.enc_values_supported");
		if (encValuesEl == null || !encValuesEl.isJsonArray()) {
			throw error("Required credential_issuer_metadata.credential_response_encryption.enc_values_supported is missing or not a JSON array",
				args("enc_values_supported", encValuesEl));
		}
		validateEncValuesSupported(encValuesEl.getAsJsonArray());

		JsonElement encryptionRequiredEl = env.getElementFromObject("vci",
			"credential_issuer_metadata.credential_response_encryption.encryption_required");
		validateEncryptionRequired(encryptionRequiredEl);

		JsonElement zipValuesEl = env.getElementFromObject("vci",
			"credential_issuer_metadata.credential_response_encryption.zip_values_supported");
		if (zipValuesEl != null) {
			validateZipValuesSupported(zipValuesEl);
		}

		logSuccess("Checked credential_response_encryption metadata",
			args("alg_values_supported", algValuesEl,
				"enc_values_supported", encValuesEl));

		return env;
	}

	/**
	 * Per OID4VCI 1.0 Final §12.2.4: "alg_values_supported: REQUIRED. A non-empty array
	 * containing a list of the JWE encryption algorithms …". Throws a test failure if the
	 * array is empty.
	 */
	protected void validateAlgValuesSupported(JsonArray algValuesSupported) {
		if (algValuesSupported.isEmpty()) {
			throw error("credential_issuer_metadata.credential_response_encryption.alg_values_supported must not be empty",
				args("alg_values_supported", algValuesSupported));
		}
	}

	/**
	 * Per OID4VCI 1.0 Final §12.2.4: "enc_values_supported: REQUIRED. A non-empty array
	 * containing a list of the JWE encryption algorithms …". Throws a test failure if the
	 * array is empty.
	 */
	protected void validateEncValuesSupported(JsonArray encValuesSupported) {
		if (encValuesSupported.isEmpty()) {
			throw error("credential_issuer_metadata.credential_response_encryption.enc_values_supported must not be empty",
				args("enc_values_supported", encValuesSupported));
		}
	}

	/**
	 * Per OID4VCI 1.0 Final §12.2.4: "encryption_required: REQUIRED. Boolean value specifying
	 * whether the Credential Issuer requires the additional encryption on top of TLS for the
	 * Credential Response." Throws a test failure if the field is missing or not a JSON boolean.
	 */
	protected void validateEncryptionRequired(JsonElement encryptionRequiredEl) {
		if (encryptionRequiredEl == null
			|| !encryptionRequiredEl.isJsonPrimitive()
			|| !encryptionRequiredEl.getAsJsonPrimitive().isBoolean()) {
			throw error("Required credential_issuer_metadata.credential_response_encryption.encryption_required is missing or not a JSON boolean",
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
			throw error("credential_issuer_metadata.credential_response_encryption.zip_values_supported must not be an empty JSON array",
				args("zip_values_supported", zipValuesSupported));
		}
	}
}
