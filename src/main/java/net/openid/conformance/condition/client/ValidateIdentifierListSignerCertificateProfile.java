package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Validates the leaf certificate of an identifier list's x5chain against the MSO revocation list
 * signer certificate profile in ISO/IEC 18013-5 Table B.9.
 *
 * <p>12.3.6.3's requirements, Table B.9 included, apply to "the MSO revocation list for both the
 * identifier list and status list mechanism", so this is
 * {@link ValidateStatusListSignerCertificateProfile} reading the identifier list token instead of
 * the status list token.
 */
public class ValidateIdentifierListSignerCertificateProfile extends ValidateStatusListSignerCertificateProfile {

	@Override
	protected String getTokenEnvKey() {
		return AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_TOKEN;
	}

	@Override
	@PreEnvironment(strings = { AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_TOKEN })
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
