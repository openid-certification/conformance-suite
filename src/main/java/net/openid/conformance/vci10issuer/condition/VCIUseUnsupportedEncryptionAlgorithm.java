package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Modifies the credential_response_encryption in the credential request to use an
 * unsupported encryption algorithm ("UNSUPPORTED_ALG").
 *
 * This is used for negative testing to verify that the issuer properly rejects
 * requests with unsupported encryption parameters and returns the
 * invalid_encryption_parameters error.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.3.1">OID4VCI Section 8.3.1 - Credential Error Response</a>
 */
public class VCIUseUnsupportedEncryptionAlgorithm extends AbstractCondition {

	private static final String UNSUPPORTED_ALG = "UNSUPPORTED_ALG";

	@Override
	@PreEnvironment(required = "vci_credential_request_object")
	@PostEnvironment(required = "vci_credential_request_object")
	public Environment evaluate(Environment env) {

		JsonObject credentialRequest = env.getObject("vci_credential_request_object");

		if (!credentialRequest.has("credential_response_encryption")) {
			throw error("credential_response_encryption not found in credential request. This condition requires encryption to be enabled.",
				args("credential_request", credentialRequest));
		}

		JsonObject encryptionParams = credentialRequest.getAsJsonObject("credential_response_encryption");
		String originalAlg = encryptionParams.has("alg") ? OIDFJSON.getString(encryptionParams.get("alg")) : null;

		// Replace the algorithm with an unsupported one
		encryptionParams.addProperty("alg", UNSUPPORTED_ALG);

		logSuccess("Replaced encryption algorithm with unsupported value",
			args("original_alg", originalAlg, "new_alg", UNSUPPORTED_ALG));

		return env;
	}
}
