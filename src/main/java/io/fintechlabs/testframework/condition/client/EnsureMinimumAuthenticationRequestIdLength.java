package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureMinimumAuthenticationRequestIdLength extends AbstractCondition {

	private final double requiredLength = 128;

	public EnsureMinimumAuthenticationRequestIdLength(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {
		String accessToken = env.getString("backchannel_authentication_endpoint_response", "auth_req_id");

		if (Strings.isNullOrEmpty(accessToken)) {
			throw error("Can't find auth_req_id");
		}

		byte[] bytes = accessToken.getBytes();

		int bitLength = bytes.length * 8;

		if (bitLength >= requiredLength) {
			logSuccess("Authentication request ID is of sufficient length", args("required", requiredLength, "actual", bitLength));
			return env;
		} else {
			throw error("Authentication request ID is not of sufficient length", args("required", requiredLength, "actual", bitLength));
		}
	}
}
