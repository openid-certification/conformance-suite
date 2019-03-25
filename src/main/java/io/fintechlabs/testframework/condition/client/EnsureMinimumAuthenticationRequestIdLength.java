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
