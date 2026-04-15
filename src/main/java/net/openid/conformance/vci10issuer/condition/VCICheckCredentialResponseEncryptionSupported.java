package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;

/**
 * Validates the credential_response_encryption block of credential issuer metadata, if present.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-12.2.4">OID4VCI Section 12.2.4 - Credential Issuer Metadata</a>
 */
public class VCICheckCredentialResponseEncryptionSupported extends AbstractVCICheckEncryptionMetadataSupported {

	@Override
	protected String getMetadataKey() {
		return "credential_response_encryption";
	}

	@Override
	protected void checkDirectionSpecificFields(JsonObject encryptionMetadata) {
		requireNonEmptyArray(encryptionMetadata, "alg_values_supported");
	}
}
