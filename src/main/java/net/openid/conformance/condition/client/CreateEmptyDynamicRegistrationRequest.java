package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateEmptyDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		// create an empty JSON to act as the registration request
		JsonObject dynamicRegistrationRequest = new JsonObject();

		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		log("Created empty dynamic registration request");

		return env;
	}
}
