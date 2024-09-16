package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckNonceLength extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"nonce"})
	public Environment evaluate(Environment env) {
		final int MAX_LEN = 43;

		String nonce = env.getString("nonce");

		if (! Strings.isNullOrEmpty(nonce)) {

			if (nonce.length() > MAX_LEN) {
				throw error("Nonce contains in excess of %d characters. This may introduce interoperability issues.".formatted(MAX_LEN));
			}
		}

		logSuccess("Nonce is empty or does not exceed %d characters".formatted(MAX_LEN));
		return env;
	}
}
