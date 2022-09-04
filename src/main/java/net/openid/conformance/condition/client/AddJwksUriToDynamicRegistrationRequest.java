package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddJwksUriToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request", strings = "jwks_uri")
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");

		String jwksUri = env.getString("jwks_uri");

		dynamicRegistrationRequest.addProperty("jwks_uri", jwksUri);

		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		log("Added jwks_uri to dynamic registration request", args("dynamic_registration_request", dynamicRegistrationRequest));

		return env;
	}

}
