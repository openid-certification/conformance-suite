package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that the vct claim in the issued SD-JWT VC matches the 'vct' of the credential
 * configuration the wallet requested. OID4VCI A.3.2 defines 'vct' as a REQUIRED credential
 * issuer metadata parameter for SD-JWT VC configurations; the spec has no verbatim clause
 * that the issued credential's vct must equal it, but a mismatch means the issuer returned
 * a different credential type than was requested.
 *
 * This is the SD-JWT VC counterpart of {@link EnsureMdocDocTypeMatchesCredentialConfiguration}.
 */
public class EnsureSdJwtVcVctMatchesCredentialConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "sdjwt", "vci_credential_configuration" })
	public Environment evaluate(Environment env) {

		String expectedVct = env.getString("vci_credential_configuration", "vct");
		if (expectedVct == null) {
			throw error("The credential configuration in the issuer metadata does not contain the 'vct' parameter, "
					+ "which is REQUIRED for SD-JWT VC credential configurations",
				args("credential_configuration", env.getObject("vci_credential_configuration")));
		}

		String vct = env.getString("sdjwt", "credential.claims.vct");
		if (vct == null) {
			throw error("The issued SD-JWT VC does not contain a 'vct' claim, so it cannot be checked against "
					+ "the requested credential configuration",
				args("expected_vct", expectedVct));
		}

		if (!expectedVct.equals(vct)) {
			throw error("The vct claim in the issued SD-JWT VC does not match the 'vct' of the requested "
					+ "credential configuration - the issuer returned a different credential type than was requested",
				args("expected_vct", expectedVct, "actual_vct", vct));
		}

		logSuccess("The vct claim in the issued SD-JWT VC matches the 'vct' of the requested credential configuration",
			args("vct", vct));

		return env;
	}
}
