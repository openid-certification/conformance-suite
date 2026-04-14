package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Ensures that credential request and response encryption metadata are declared together.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2">OID4VCI Section 8.2 - Credential Request</a>
 */
public class VCIEnsureCredentialEncryptionMetadataIsConsistent extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		boolean responseDeclared = env.getElementFromObject("vci",
			"credential_issuer_metadata.credential_response_encryption") != null;
		boolean requestDeclared = env.getElementFromObject("vci",
			"credential_issuer_metadata.credential_request_encryption") != null;

		if (responseDeclared && !requestDeclared) {
			throw error("Credential issuer metadata declares credential_response_encryption but is missing "
				+ "credential_request_encryption.");
		}

		if (!responseDeclared && requestDeclared) {
			throw error("Credential issuer metadata declares credential_request_encryption but is missing "
				+ "credential_response_encryption.");
		}

		logSuccess("credential_request_encryption and credential_response_encryption metadata are consistent",
			args("credential_response_encryption_declared", responseDeclared,
				"credential_request_encryption_declared", requestDeclared));

		return env;
	}
}
