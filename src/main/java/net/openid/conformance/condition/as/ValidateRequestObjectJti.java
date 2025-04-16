package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateRequestObjectJti extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_request_object"})
	public Environment evaluate(Environment env) {
		JsonElement jtiElement = env.getElementFromObject("authorization_request_object", "claims.jti");

		if (jtiElement == null) {
			throw error("Missing jti, request object does not contain a 'jti' claim");
		}

		if (!jtiElement.isJsonPrimitive() || !jtiElement.getAsJsonPrimitive().isString()) {
			throw error("'jti' claim in request object is not a string", args("jti", jtiElement));
		}

		if (OIDFJSON.getString(jtiElement).isEmpty()) {
			throw error("'jti' claim in request object cannot be an empty string", args("jti", jtiElement));
		}

		logSuccess("jti claim is a non-empty string", args("jti", jtiElement));
		return env;
	}

}
