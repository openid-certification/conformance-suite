package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckIfBackchannelAuthenticationEndpointResponseError extends AbstractCondition {

	public CheckIfBackchannelAuthenticationEndpointResponseError(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {

		if (!Strings.isNullOrEmpty(env.getString("backchannel_authentication_endpoint_response", "error"))) {
			throw error("Backchannel authentication endpoint error response", env.getObject("backchannel_authentication_endpoint_response"));
		} else {
			logSuccess("No error from Backchannel authentication endpoint");
			return env;
		}

	}

}
