package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractEnsureMinimumEntropy;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureRecommendedAuthenticationRequestIdEntropy extends AbstractEnsureMinimumEntropy {

	private final double recommendedEntropy = 160;

	public EnsureRecommendedAuthenticationRequestIdEntropy(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	public Environment evaluate(Environment env) {

		String authRequestId = env.getString("backchannel_authentication_endpoint_response", "auth_req_id");

		if (Strings.isNullOrEmpty(authRequestId)) {
			throw error("auth_req_id was not present in the backchannel authentication endpoint response.");
		}

		double bitsPerCharacter = getShannonEntropy(authRequestId);

		double entropy = bitsPerCharacter * (double) authRequestId.length();

		if (entropy > recommendedEntropy) {
			logSuccess("Calculated entropy", args("recommended", recommendedEntropy, "actual", entropy));
			return env;
		} else {
			throw error("Entropy not met recommended", args("recommended", recommendedEntropy, "actual", entropy));
		}
	}
}
