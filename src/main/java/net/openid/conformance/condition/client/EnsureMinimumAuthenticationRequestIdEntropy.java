package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractEnsureMinimumEntropy;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMinimumAuthenticationRequestIdEntropy extends AbstractEnsureMinimumEntropy {
	/**
	 * The actual amount of required entropy is 128 bits, but we can't accurately measure entropy so a bit of
	 * slop is allowed for.
	 */
	private final double requiredEntropy = 96;

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {
		String authRequestId = env.getString("backchannel_authentication_endpoint_response", "auth_req_id");

		if (Strings.isNullOrEmpty(authRequestId)) {
			throw error("auth_req_id was not present in the backchannel authentication endpoint response.");
		}

		return ensureMinimumEntropy(env, authRequestId, requiredEntropy);
	}
}
