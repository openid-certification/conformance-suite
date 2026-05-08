package net.openid.conformance.vci10wallet.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCheckEndpointContentTypeReturned;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

/**
 * Enforces OID4VCI 1.0 Final § 8.2-11 (and § 9.1-8 for the deferred endpoint): if the credential
 * request is not encrypted, the media type MUST be {@code application/json}.
 */
public class VCIEnsureCredentialRequestUsesApplicationJson extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {
		String contentType = env.getString("incoming_request", "headers.content-type");
		String mimeType = (contentType == null || contentType.isBlank())
			? null
			: AbstractCheckEndpointContentTypeReturned.getMimeTypeFromContentType(contentType);

		if (!"application/json".equalsIgnoreCase(mimeType)) {
			String errorDescription = "Plaintext credential request Content-Type must be application/json";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_CREDENTIAL_REQUEST, errorDescription);
			throw error(errorDescription, args("content_type", contentType));
		}

		logSuccess("Credential request Content-Type is application/json", args("content_type", contentType));
		return env;
	}
}
