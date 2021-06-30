package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureRequestObjectWasNotEncrypted extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_request_object"})
	public Environment evaluate(Environment env) {
		JsonElement jweHeaderElement = env.getElementFromObject("authorization_request_object", "jwe_header");
		if(jweHeaderElement != null) {
			throw error("Request object was encrypted, this test expects an unencrypted request object",
				args("jwe_header", jweHeaderElement));
		} else {
			logSuccess("Request object was not encrypted");
		}
		return env;
	}

}
