package net.openid.conformance.vci10issuer.condition;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Resolves the deferred credential endpoint URL from the credential issuer metadata.
 *
 * Per OID4VCI Section 12.2.3, the deferred_credential_endpoint is an optional metadata field
 * that contains the URL of the deferred credential endpoint.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-12.2.3">OID4VCI Section 12.2.3 - Credential Issuer Metadata</a>
 */
public class VCIResolveDeferredCredentialEndpointToUse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		String deferredCredentialEndpointUrl = env.getString("vci", "credential_issuer_metadata.deferred_credential_endpoint");

		if (Strings.isNullOrEmpty(deferredCredentialEndpointUrl)) {
			throw error("deferred_credential_endpoint not found in credential issuer metadata. " +
				"This is required when the issuer returns a deferred response (transaction_id).",
				args("credential_issuer_metadata", env.getElementFromObject("vci", "credential_issuer_metadata")));
		}

		log("Use deferred credential endpoint from credential issuer metadata",
			args("deferred_credential_endpoint", deferredCredentialEndpointUrl));
		env.putString("resource", "resourceUrl", deferredCredentialEndpointUrl);

		return env;
	}
}
