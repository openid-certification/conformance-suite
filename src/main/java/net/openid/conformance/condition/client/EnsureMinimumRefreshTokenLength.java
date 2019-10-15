package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMinimumRefreshTokenLength extends AbstractCondition {

	private final int requiredLength = 128;

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		String refreshToken = env.getString("token_endpoint_response", "refresh_token");

		if (Strings.isNullOrEmpty(refreshToken)) {
			throw error("Can't find refresh token");
		}

		byte[] bytes = refreshToken.getBytes();

		int bitLength = bytes.length * 8;

		if (bitLength >= requiredLength) {
			logSuccess("Refresh token is of sufficient length", args("required", requiredLength, "actual", bitLength));
			return env;
		} else {
			throw error("Refresh token is not of sufficient length", args("required", requiredLength, "actual", bitLength));
		}

	}

}
