package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureRequestObjectWasEncrypted extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_request_object"})
	public Environment evaluate(Environment env) {
		JsonElement jweHeaderElement = env.getElementFromObject("authorization_request_object", "jwe_header");
		if(jweHeaderElement == null) {
			throw error("Request object was not encrypted");
		} else {
			logSuccess("Request object was encrypted", args("jwe_header", jweHeaderElement));
		}
		return env;
	}

}
