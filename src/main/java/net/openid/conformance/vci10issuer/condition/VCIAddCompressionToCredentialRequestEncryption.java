package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Adds DEFLATE compression ("zip": "DEF") to an existing credential_response_encryption
 * object in the credential request.
 *
 * This is used to test compression support separately from basic encryption.
 * When inserted after VCIAddCredentialResponseEncryptionToRequest and before
 * SerializeVCICredentialRequestObject, the request has not yet been serialized,
 * so no re-serialization is needed.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2">OID4VCI Section 8.2 - Credential Request</a>
 */
public class VCIAddCompressionToCredentialRequestEncryption extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci_credential_request_object")
	public Environment evaluate(Environment env) {

		JsonObject credentialRequest = env.getObject("vci_credential_request_object");

		JsonObject credentialResponseEncryption = credentialRequest.getAsJsonObject("credential_response_encryption");
		if (credentialResponseEncryption == null) {
			throw error("credential_response_encryption not found in credential request; "
				+ "VCIAddCredentialResponseEncryptionToRequest must be called first");
		}

		credentialResponseEncryption.addProperty("zip", "DEF");

		logSuccess("Added zip=DEF to credential_response_encryption",
			args("credential_response_encryption", credentialResponseEncryption));

		return env;
	}
}
