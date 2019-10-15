package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractEnsureMinimumEntropy;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMinimumRefreshTokenEntropy extends AbstractEnsureMinimumEntropy {
	/**
	 * The actual amount of required entropy is 128 bits, but we can't accurately measure entropy so a bit of
	 * slop is allowed for.
	 */
	private final double requiredEntropy = 96;

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		String refreshToken = env.getString("token_endpoint_response", "refresh_token");

		if (Strings.isNullOrEmpty(refreshToken)) {
			throw error("Can't find refresh token");
		}

		return ensureMinimumEntropy(env, refreshToken, requiredEntropy);
	}

}
