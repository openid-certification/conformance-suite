package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilEnsureRegistrationRequestDoesNotIncludeJwks extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request"})
	public Environment evaluate(Environment env) {

		JsonObject request = env.getObject("dynamic_registration_request");
		if(request.has("jwks")) {
			throw error("Registration request must not contain a jwks (key set by value)", args("dynamic_registration_request", request));
		}
		logSuccess("Registration request does not contain a jwks");
		return env;
	}
}
