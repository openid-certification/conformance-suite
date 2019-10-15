package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateAuthenticationRequestIdExpiresIn extends AbstractCondition {

	private final double maximumExpiresIn = 356 * 24 * 60 * 60; // 1 year as 30758400 seconds

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

		int expiresIn = OIDFJSON.getInt(jExpiresIn);
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
