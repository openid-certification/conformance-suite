package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMinimumAuthorizationCodeLength extends AbstractCondition {

	private final int requiredLength = 128;

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		String authorizationCode = env.getString("authorization_endpoint_response", "code");

		if (Strings.isNullOrEmpty(authorizationCode)) {
			throw error("Can't find authorization code");
		}

		byte[] bytes = authorizationCode.getBytes();

		int bitLength = bytes.length * 8;

		if (bitLength >= requiredLength) {
			logSuccess("Authorization code is of sufficient length", args("required", requiredLength, "actual", bitLength));
			return env;
		} else {
			throw error("Authorization code is not long enough", args("required_bits", requiredLength, "actual_bits", bitLength));
		}

	}

}
