package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMinimumAuthenticationRequestIdLength extends AbstractCondition {

	private final int requiredLength = 128;

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {
		String authRequestId = env.getString("backchannel_authentication_endpoint_response", "auth_req_id");

		if (Strings.isNullOrEmpty(authRequestId)) {
			throw error("auth_req_id was not present in the backchannel authentication endpoint response.");
		}

		byte[] bytes = authRequestId.getBytes();

		int bitLength = bytes.length * 8;

		if (bitLength >= requiredLength) {
			logSuccess("auth_req_id is of sufficient length", args("required", requiredLength, "actual", bitLength));
			return env;
		} else {
			throw error("auth_req_id is not of sufficient length", args("required", requiredLength, "actual", bitLength));
		}
	}
}
