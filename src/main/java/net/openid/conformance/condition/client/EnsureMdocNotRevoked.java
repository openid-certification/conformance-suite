package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;

/**
 * Fails when the status {@link ExtractMdocRevocationStatus} read from the MSO revocation list
 * says the MSO has been revoked. ISO/IEC 18013-5 12.3.6.1: for an mdoc "no other status besides
 * 'revoked' shall be used", so any status other than VALID means the credential is revoked.
 */
public class EnsureMdocNotRevoked extends AbstractRevocationListCwtCondition {

	@Override
	@PreEnvironment(strings = { ENV_STATUS })
	public Environment evaluate(Environment env) {
		String status = env.getString(ENV_STATUS);

		if (!TokenStatusList.Status.VALID.name().equals(status)) {
			throw error("The mdoc's MSO has been revoked according to the MSO revocation list",
				args("status", status, "uri", env.getString(ENV_URI)));
		}

		logSuccess("The mdoc's MSO is not revoked according to the MSO revocation list",
			args("status", status, "uri", env.getString(ENV_URI)));
		return env;
	}
}
