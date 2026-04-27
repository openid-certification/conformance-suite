package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractEnsureMinimumEntropy;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMinimumNonceEntropy extends AbstractEnsureMinimumEntropy {
	/**
	 * The target enforced here is 128 bits (16 bytes) of entropy, but Shannon
	 * entropy can only be estimated, so the threshold is set to 96 bits to
	 * allow some slop in the estimate.
	 */
	protected final double requiredEntropy = 96;

	@Override
	@PreEnvironment(strings = {"nonce"})
	public Environment evaluate(Environment env) {
		String nonce = env.getString("nonce");

		if (Strings.isNullOrEmpty(nonce)) {
			throw error("nonce is empty");
		}

		return ensureMinimumEntropy(env, nonce, requiredEntropy, buildSuccessMessage(), buildErrorMessage());
	}

	protected String buildSuccessMessage() {
		return "Calculated shannon entropy of nonce seems sufficient";
	}

	protected String buildErrorMessage() {
		return "Calculated shannon entropy of nonce does not seem to meet the minimum required entropy (i.e. nonce is too short or not random enough)";
	}
}
