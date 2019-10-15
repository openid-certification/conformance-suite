package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddPublicJwksToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request", "client_public_jwks"} )
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {
		JsonObject publicJwks = env.getObject("client_public_jwks");

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");

		dynamicRegistrationRequest.add("jwks", publicJwks);

		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		log("Added client public JWKS to dynamic registration request", args("dynamic_registration_request", dynamicRegistrationRequest));

		return env;
	}
}
