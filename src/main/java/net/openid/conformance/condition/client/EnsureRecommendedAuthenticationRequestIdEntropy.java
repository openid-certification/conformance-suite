package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractEnsureMinimumEntropy;
import net.openid.conformance.testmodule.Environment;

public class EnsureRecommendedAuthenticationRequestIdEntropy extends AbstractEnsureMinimumEntropy {

	private final double recommendedEntropy = 160;

	@Override
	public Environment evaluate(Environment env) {

		String authRequestId = env.getString("backchannel_authentication_endpoint_response", "auth_req_id");

		if (Strings.isNullOrEmpty(authRequestId)) {
			throw error("auth_req_id was not present in the backchannel authentication endpoint response.");
		}

		double bitsPerCharacter = getShannonEntropy(authRequestId);

		double entropy = bitsPerCharacter * (double) authRequestId.length();

		if (entropy > recommendedEntropy) {
			logSuccess("Calculated entropy", args("value", authRequestId, "recommended", recommendedEntropy, "actual", entropy));
			return env;
		} else {
			throw error("Entropy not met recommended", args("value", authRequestId, "recommended", recommendedEntropy, "actual", entropy));
		}
	}
}
