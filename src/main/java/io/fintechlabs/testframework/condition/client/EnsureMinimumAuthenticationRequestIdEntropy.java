package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractEnsureMinimumEntropy;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureMinimumAuthenticationRequestIdEntropy extends AbstractEnsureMinimumEntropy {
	/**
	 * The actual amount of required entropy is 128 bits, but we can't accurately measure entropy so a bit of
	 * slop is allowed for.
	 */
	private final double requiredEntropy = 96;

	public EnsureMinimumAuthenticationRequestIdEntropy(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {
		String accessToken = env.getString("backchannel_authentication_endpoint_response", "auth_req_id");

		if (Strings.isNullOrEmpty(accessToken)) {
			throw error("Can't find auth_req_id");
		}

		return ensureMinimumEntropy(env, accessToken, requiredEntropy);
	}
}
