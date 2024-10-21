package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckNonceMinimumLength extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"nonce"})
	public Environment evaluate(Environment env) {
		final int MIN_LEN = 16; // there's no clear guidance but this is only for a warning

		String nonce = env.getString("nonce");

		if (nonce.length() < MIN_LEN) {
			throw error("Nonce is shorter than %d characters. This is unlikely to be sufficient entropy.".formatted(MIN_LEN));
		}

		logSuccess("Nonce is at least %d characters".formatted(MIN_LEN));
		return env;
	}
}
