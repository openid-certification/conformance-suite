package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCheckEndpointContentTypeReturned;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

/**
 * Enforces OID4VCI 1.0 Final § 8.2-9 at the Content-Type layer: if the issuer's metadata sets
 * {@code credential_request_encryption.encryption_required} to {@code true}, the credential
 * request MUST use Content-Type {@code application/jwt}. Body validity is verified by
 * {@link VCIDecryptCredentialRequest}.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2">OID4VCI § 8.2</a>
 */
public class VCIEnsureCredentialRequestUsesApplicationJwtIfIssuerRequiresEncryption extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"incoming_request", "credential_issuer_metadata"})
	public Environment evaluate(Environment env) {

		JsonElement credentialRequestEncryption = env.getElementFromObject("credential_issuer_metadata",
			"credential_request_encryption");
		JsonElement encryptionRequiredEl = env.getElementFromObject("credential_issuer_metadata",
			"credential_request_encryption.encryption_required");
		boolean issuerRequiresEncryption = encryptionRequiredEl != null
			&& encryptionRequiredEl.isJsonPrimitive()
			&& encryptionRequiredEl.getAsJsonPrimitive().isBoolean()
			&& OIDFJSON.getBoolean(encryptionRequiredEl);

		String contentType = env.getString("incoming_request", "headers.content-type");

		if (!issuerRequiresEncryption) {
			logSuccess("Credential issuer metadata does not require credential request encryption",
				args("content_type", contentType,
					"credential_request_encryption", credentialRequestEncryption));
			return env;
		}

		String mimeType = (contentType == null || contentType.isBlank())
			? null
			: AbstractCheckEndpointContentTypeReturned.getMimeTypeFromContentType(contentType);

		if (!"application/jwt".equalsIgnoreCase(mimeType)) {
			String errorDescription = "Issuer requires credential request encryption but Content-Type is not application/jwt";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_CREDENTIAL_REQUEST, errorDescription);
			throw error(errorDescription, args("content_type", contentType,
				"credential_request_encryption", credentialRequestEncryption));
		}

		logSuccess("Credential request Content-Type is application/jwt as required by the issuer's metadata",
			args("content_type", contentType,
				"credential_request_encryption", credentialRequestEncryption));
		return env;
	}
}
