package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractEnsureMinimumEntropy;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMinimumAuthorizationCodeEntropy extends AbstractEnsureMinimumEntropy {
	/**
	 * The actual amount of required entropy is 128 bits, but we can't accurately measure entropy so a bit of
	 * slop is allowed for.
	 */
	private final double requiredEntropy = 96;

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		String authorizationCode = env.getString("authorization_endpoint_response", "code");

		if (Strings.isNullOrEmpty(authorizationCode)) {
			throw error("Can't find authorization code");
		}

		return ensureMinimumEntropy(env, authorizationCode, requiredEntropy);
	}

}
