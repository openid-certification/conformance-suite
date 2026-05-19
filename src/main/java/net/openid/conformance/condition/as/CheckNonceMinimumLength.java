package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * The relevant authorization specs do not impose a minimum nonce length. This
 * check flags obviously-too-short nonces; values shorter than 16 characters
 * cannot plausibly carry sufficient entropy regardless of alphabet. The
 * substantive entropy check is performed by EnsureMinimumNonceEntropy.
 * Subclasses may override the message-building methods to add spec-specific
 * detail.
 */
public class CheckNonceMinimumLength extends AbstractCondition {

	protected static final int MIN_LEN = 16;

	@Override
	@PreEnvironment(strings = {"nonce"})
	public Environment evaluate(Environment env) {
		String nonce = env.getString("nonce");

		if (nonce.length() < MIN_LEN) {
			throw error(buildTooShortMessage());
		}

		logSuccess(buildSuccessMessage());
		return env;
	}

	protected String buildTooShortMessage() {
		return ("Nonce is shorter than %d characters. Values this short cannot plausibly carry sufficient entropy " +
			"regardless of alphabet.").formatted(MIN_LEN);
	}

	protected String buildSuccessMessage() {
		return "Nonce is at least %d characters (the floor below which a nonce cannot plausibly carry sufficient entropy).".formatted(MIN_LEN);
	}
}
