package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractEnsureMinimumEntropy;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMinimumNonceEntropy extends AbstractEnsureMinimumEntropy {
	/**
	 * The actual amount of required entropy is 128 bits (16 bytes), but we can't accurately measure
	 * entropy so a bit of slop is allowed for.
	 */
	private final double requiredEntropy = 96;

	@Override
	@PreEnvironment(strings = {"nonce"})
	public Environment evaluate(Environment env) {
		String nonce = env.getString("nonce");

		if (Strings.isNullOrEmpty(nonce)) {
			throw error("nonce is empty");
		}

		return ensureMinimumEntropy(env, nonce, requiredEntropy);
	}
}
