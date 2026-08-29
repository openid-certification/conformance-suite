package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Verifies the COSE_Sign1 signature on an identifier list using the public key of the leaf
 * certificate of the x5chain in its protected header.
 *
 * <p>ISO/IEC 18013-5 12.3.6.3 states the x5chain requirement for both MSO revocation list
 * mechanisms, so this is {@link VerifyStatusListTokenCwtSignature} reading the identifier list
 * token instead of the status list token.
 */
public class VerifyIdentifierListTokenCwtSignature extends VerifyStatusListTokenCwtSignature {

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
