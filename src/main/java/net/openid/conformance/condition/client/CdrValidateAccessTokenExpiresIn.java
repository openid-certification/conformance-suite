package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CdrValidateAccessTokenExpiresIn extends AbstractCondition {

	@Override
	@PreEnvironment(required = "expires_in")
	public Environment evaluate(Environment env) {

		JsonObject expiresIn = env.getObject("expires_in");
		JsonElement je = expiresIn.get("expires_in");
		try {
			JsonPrimitive jp = je.getAsJsonPrimitive();
			if (!jp.isNumber()) {
				throw error("expires_in is not a number");
			}

			Number n = OIDFJSON.getNumber(jp);
			if (n.intValue() < 120 || n.intValue() > 600) {
				throw error("CDR requires access tokens to expire between 2 minutes and 10 minutes after issue",
					args("expires_in", jp));
			}

		} catch (IllegalStateException ex) {
			throw error("expires_in is not a JSON primitive");
		}

		logSuccess("expires_in is between 120 and 600 seconds", expiresIn);
		return env;

	}

}
