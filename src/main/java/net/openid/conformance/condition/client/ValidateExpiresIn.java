package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateExpiresIn extends AbstractCondition {

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
				// https://tools.ietf.org/html/rfc6749#appendix-A.14 technically allows a zero expires_in, but
				// returning a token that is already expires seems nonsensical
				throw error("expires_in must be positive");
			}

		} catch (IllegalStateException ex) {
			throw error("expires_in is not a JSON primitive");
		}

		logSuccess("expires_in passed all validation checks",expiresIn);
		return env;

	}

}
