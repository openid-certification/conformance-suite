package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractEnsureMinimumEntropy;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMinimumNonceEntropy extends AbstractEnsureMinimumEntropy {

	@Override
	@PreEnvironment(strings = {"nonce"})
	public Environment evaluate(Environment env) {
		String nonce = env.getString("nonce");

		if (Strings.isNullOrEmpty(nonce)) {
			throw error("nonce is empty");
		}

		return ensureMinimumEntropy(env, nonce, getRequiredEntropy(), buildSuccessMessage(), buildErrorMessage());
	}

	/**
	 * The threshold, in bits, that the Shannon estimate (bits per character
	 * times length) must exceed. The estimate is limited by the string's length
	 * and alphabet, so the threshold must sit below what correctly generated
	 * nonces of the expected length measure, not at their true entropy.
	 */
	protected double getRequiredEntropy() {
		return 96;
	}

	protected String buildSuccessMessage() {
		return "Calculated shannon entropy of nonce seems sufficient";
	}

	protected String buildErrorMessage() {
		return "Calculated shannon entropy of nonce does not seem to meet the minimum required entropy (i.e. nonce is too short or not random enough)";
	}
}
