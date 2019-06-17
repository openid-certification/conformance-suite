package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddTokenEndpointAuthSigningAlgPS256ToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request")
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");

		dynamicRegistrationRequest.addProperty("token_endpoint_auth_signing_alg", "PS256");

		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		log("Added PS256 for token endpoint auth signing alg to dynamic registration request", args("dynamic_registration_request", dynamicRegistrationRequest));

		return env;
	}
}
