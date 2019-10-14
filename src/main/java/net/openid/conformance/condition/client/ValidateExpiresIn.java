package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateExpiresIn extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {}, required = "expires_in")
	public Environment evaluate(Environment env) {

		JsonObject expiresIn = env.getObject("expires_in");
		JsonElement je = expiresIn.get("expires_in");
		try {
			JsonPrimitive jp = je.getAsJsonPrimitive();
			if (!jp.isNumber()) {
				logFailure(expiresIn);
				throw error("expires_in, is not a Number!");
			}

		} catch (IllegalStateException ex) {
			logFailure(expiresIn);
			throw error("expires_in, is not a primitive!");
		}

		logSuccess("expires_in passed all validation checks",expiresIn);
		return env;

	}

}
