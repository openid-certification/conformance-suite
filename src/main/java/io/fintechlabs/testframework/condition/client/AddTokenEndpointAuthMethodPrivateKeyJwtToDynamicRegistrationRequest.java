package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddTokenEndpointAuthMethodPrivateKeyJwtToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request")
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");

		dynamicRegistrationRequest.addProperty("token_endpoint_auth_method", "private_key_jwt");

		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		log("Added private_key_jwt for token endpoint auth method to dynamic registration request", args("dynamic_registration_request", dynamicRegistrationRequest));

		return env;
	}
}
