package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMinimumAccessTokenLength extends AbstractCondition {

	private final int requiredLength = 128;

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		String accessToken = env.getString("token_endpoint_response", "access_token");

		if (Strings.isNullOrEmpty(accessToken)) {
			throw error("Can't find access token");
		}

		byte[] bytes = accessToken.getBytes();

		int bitLength = bytes.length * 8;

		if (bitLength >= requiredLength) {
			logSuccess("Access token is of sufficient length", args("required", requiredLength, "actual", bitLength));
			return env;
		} else {
			throw error("Access token is not of sufficient length", args("required", requiredLength, "actual", bitLength));
		}

	}

}
