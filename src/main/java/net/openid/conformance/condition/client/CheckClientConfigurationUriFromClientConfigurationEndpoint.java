package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckClientConfigurationUriFromClientConfigurationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "registration_client_endpoint_response", "client" })
	public Environment evaluate(Environment env) {

		String expectedRegistrationClientUri = env.getString("client", "registration_client_uri");

		String registrationClientUri = getStringFromEnvironment(env,
			"registration_client_endpoint_response",
			"body_json.registration_client_uri",
			"registration_client_uri in client configuration endpoint response");

		if (!registrationClientUri.equals(expectedRegistrationClientUri)) {
			throw error("registration_client_uri in client configuration response does not match the one in the initial registration response",
				args("registration_client_uri", registrationClientUri, "expected", expectedRegistrationClientUri));
		}

		logSuccess("Client configuration endpoint returned correct registration_client_uri.");

		return env;
	}
}
