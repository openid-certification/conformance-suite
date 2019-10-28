package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddTokenEndpointAuthMethodToDynamicRegistrationRequestFromEnvironment extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request", strings = "client_auth_type")
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");
		String clientAuthType = env.getString("client_auth_type");

		dynamicRegistrationRequest.addProperty("token_endpoint_auth_method", clientAuthType);

		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		log("Added token endpoint auth method to dynamic registration request", args("dynamic_registration_request", dynamicRegistrationRequest));

		return env;
	}
}
