package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateAuthenticationRequestIdInterval extends AbstractCondition {

	public ValidateAuthenticationRequestIdInterval(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement jInterval = env.getElementFromObject("backchannel_authentication_endpoint_response", "interval");

		if (jInterval == null) {
			log("interval is empty.");
			return env;
		}

		if (!jInterval.isJsonPrimitive()) {
			throw error("interval is not a primitive!");
		}

		if (!jInterval.getAsJsonPrimitive().isNumber()) {
			throw error("interval is not a number!");
		}

		int interval = jInterval.getAsJsonPrimitive().getAsInt();
		if (interval < 0) {
			throw error("interval is less than zero");
		}

		logSuccess("interval passed all validation checks", args("interval", interval));

		return env;
	}
}
