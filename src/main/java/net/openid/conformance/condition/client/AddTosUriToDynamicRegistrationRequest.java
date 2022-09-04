package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddTosUriToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request", strings = "tos_uri")
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");

		String tosUri = env.getString("tos_uri");

		dynamicRegistrationRequest.addProperty("tos_uri", tosUri);

		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		log("Added tos_uri to dynamic registration request", args("dynamic_registration_request", dynamicRegistrationRequest));

		return env;
	}

}
