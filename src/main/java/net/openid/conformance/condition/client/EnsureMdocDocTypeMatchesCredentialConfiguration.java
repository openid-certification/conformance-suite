package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that the docType in the issued mdoc credential's Mobile Security Object matches the
 * 'doctype' of the credential configuration the wallet requested. OID4VCI A.2.2 defines
 * 'doctype' as a REQUIRED credential issuer metadata parameter for mso_mdoc configurations;
 * the spec has no verbatim clause that the issued credential's docType must equal it, but a
 * mismatch means the issuer returned a different credential type than was requested.
 *
 * Reads the docType stored in 'mdoc_doctype' by ParseMdocCredentialFromVCIIssuance.
 */
public class EnsureMdocDocTypeMatchesCredentialConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "mdoc_doctype", required = "vci_credential_configuration")
	public Environment evaluate(Environment env) {

		String expectedDocType = env.getString("vci_credential_configuration", "doctype");
		if (expectedDocType == null) {
			throw error("The credential configuration in the issuer metadata does not contain the 'doctype' parameter, "
					+ "which is REQUIRED for mso_mdoc credential configurations",
				args("credential_configuration", env.getObject("vci_credential_configuration")));
		}

		String docType = env.getString("mdoc_doctype");

		if (!expectedDocType.equals(docType)) {
			throw error("The docType in the issued mdoc's Mobile Security Object does not match the 'doctype' of "
					+ "the requested credential configuration - the issuer returned a different credential type than was requested",
				args("expected_doctype", expectedDocType, "actual_doctype", docType));
		}

		logSuccess("The docType in the issued mdoc's Mobile Security Object matches the 'doctype' of the requested credential configuration",
			args("doctype", docType));

		return env;
	}
}
