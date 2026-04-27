package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * The relevant authorization specs do not bound the nonce length. This check
 * exists to promote interoperability: 43 characters is the length of
 * base64url(32 random bytes), the canonical "256-bit" nonce size, and is the
 * longest nonce the conformance suite itself generates in any happy-flow
 * scenario when acting on the opposite side of the protocol. Subclasses may
 * override the message-building methods to add spec-specific detail.
 */
public class CheckNonceMaximumLength extends AbstractCondition {

	protected static final int MAX_LEN = 43;

	@Override
	@PreEnvironment(strings = {"nonce"})
	public Environment evaluate(Environment env) {
		String nonce = env.getString("nonce");

		if (Strings.isNullOrEmpty(nonce)) {
			throw error("nonce is empty");
		}

		if (nonce.length() > MAX_LEN) {
			throw error(buildOverlongMessage());
		}

		logSuccess(buildSuccessMessage());
		return env;
	}

	protected String buildOverlongMessage() {
		return ("Nonce contains in excess of %d characters. To promote interoperability we expect nonces no longer than %d characters " +
			"(the longest the conformance suite generates when testing an authorization server); " +
			"longer values may not be accepted by all wallets/clients.").formatted(MAX_LEN, MAX_LEN);
	}

	protected String buildSuccessMessage() {
		return ("Nonce does not exceed %d characters " +
			"(the longest the conformance suite generates when testing an authorization server).").formatted(MAX_LEN);
	}
}
