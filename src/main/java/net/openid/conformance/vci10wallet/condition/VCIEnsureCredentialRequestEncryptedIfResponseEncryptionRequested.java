package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCheckEndpointContentTypeReturned;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

/**
 * Enforces OID4VCI 1.0 Final § 8.2-18 (and § 9.1-11 for the deferred endpoint) on the
 * plaintext path: a credential request body that includes {@code credential_response_encryption}
 * must have been sent encrypted, so a plaintext request body MUST NOT include it.
 *
 * <p>Encrypted requests satisfy the rule by construction (the request was already encrypted),
 * so this condition is only invoked from the plaintext branch of the test flow. The
 * Content-Type short-circuit in the body is defensive — current callers guarantee
 * {@code application/json} before invoking this condition.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2">OID4VCI § 8.2</a>
 */
public class VCIEnsureCredentialRequestEncryptedIfResponseEncryptionRequested extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		String contentType = env.getString("incoming_request", "headers.content-type");
		String mimeType = (contentType == null || contentType.isBlank())
			? null
			: AbstractCheckEndpointContentTypeReturned.getMimeTypeFromContentType(contentType);

		if ("application/jwt".equalsIgnoreCase(mimeType)) {
			logSuccess("Credential request was encrypted (Content-Type: application/jwt)",
				args("content_type", contentType));
			return env;
		}

		JsonElement bodyJsonEl = env.getElementFromObject("incoming_request", "body_json");
		boolean responseEncryptionRequested = bodyJsonEl != null
			&& bodyJsonEl.isJsonObject()
			&& bodyJsonEl.getAsJsonObject().has("credential_response_encryption");

		if (!responseEncryptionRequested) {
			logSuccess("Plaintext credential request body does not include credential_response_encryption",
				args("content_type", contentType));
			return env;
		}

		String errorDescription = "Plaintext credential request body includes credential_response_encryption, therefore the request must be encrypted";
		VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_CREDENTIAL_REQUEST, errorDescription);
		throw error(errorDescription, args("content_type", contentType));
	}
}
