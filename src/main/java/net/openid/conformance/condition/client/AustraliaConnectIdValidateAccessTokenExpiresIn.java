package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AustraliaConnectIdValidateAccessTokenExpiresIn extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {}, required = "expires_in")
	public Environment evaluate(Environment env) {

		JsonObject expiresIn = env.getObject("expires_in");
		JsonElement je = expiresIn.get("expires_in");
		try {
			JsonPrimitive jp = je.getAsJsonPrimitive();
			if (!jp.isNumber()) {
				throw error("expires_in is not a number");
			}

			Number n = OIDFJSON.getNumber(jp);
			if (n.intValue() <= 0) {
				throw error("expires_in is less than or equal zero");
			}

			if (n.intValue() > 600) {
				throw error("expires_in greater than 600 seconds", args("expires_in", jp));
			}

		} catch (IllegalStateException ex) {
			throw error("expires_in is not a JSON primitive");
		}

		logSuccess("expires_in no greater than 600 seconds",expiresIn);
		return env;

	}

}
