package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateAuthenticationRequestIdExpiresIn extends AbstractCondition {
	private final double maximumExpiresIn = 356 * 24 * 60 * 60; // 1 year as 30758400 seconds

	public ValidateAuthenticationRequestIdExpiresIn(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement jExpiresIn = env.getElementFromObject("backchannel_authentication_endpoint_response", "expires_in");

		if (jExpiresIn == null || !jExpiresIn.isJsonPrimitive()) {
			throw error("expires_in is not a primitive!");
		}

		if (!jExpiresIn.getAsJsonPrimitive().isNumber()) {
			throw error("expires_in is not a number!");
		}

		int expiresIn = jExpiresIn.getAsJsonPrimitive().getAsInt();
		if (expiresIn <= 0) {
			throw error("expires_in is less than or equal zero");
		}

		if (expiresIn > maximumExpiresIn) {
			throw error("expires_in is more than 1 year in the future, which is permitted by the specification but seems unreasonable.", args("expected", maximumExpiresIn, "actual", expiresIn));
		} else {
			logSuccess("expires_in passed all validation checks", args("expires_in", expiresIn));
		}

		return env;
	}
}
