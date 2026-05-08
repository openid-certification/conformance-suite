package net.openid.conformance.vci10wallet.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCheckEndpointContentTypeReturned;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

/**
 * Enforces OID4VCI 1.0 Final § 10-2: an encrypted credential / deferred credential request MUST
 * use Content-Type {@code application/jwt}. Invoked before {@link VCIDecryptCredentialRequest}
 * so that body validity is verified by decryption.
 */
public class VCIEnsureCredentialRequestUsesApplicationJwt extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {
		String contentType = env.getString("incoming_request", "headers.content-type");
		String mimeType = (contentType == null || contentType.isBlank())
			? null
			: AbstractCheckEndpointContentTypeReturned.getMimeTypeFromContentType(contentType);

		if (!"application/jwt".equalsIgnoreCase(mimeType)) {
			String errorDescription = "Encrypted credential request Content-Type must be application/jwt";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_CREDENTIAL_REQUEST, errorDescription);
			throw error(errorDescription, args("content_type", contentType));
		}

		logSuccess("Credential request Content-Type is application/jwt", args("content_type", contentType));
		return env;
	}
}
