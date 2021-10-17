package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CheckClientConfigurationCredentialsFromClientConfigurationEndpoint extends AbstractCondition {

    @Override
    @PreEnvironment(required = { "registration_client_endpoint_response",  "client" })
    public Environment evaluate(Environment env) {

		String expectedRegistrationClientUri = env.getString("registration_client_uri");
		String expectedRegistrationAccessToken = env.getString("registration_access_token");

		String registrationClientUri = getStringFromEnvironment(env,
			"registration_client_endpoint_response",
			"body_json.registration_client_uri",
			"registration_client_uri in client configuration endpoint response");
		String registrationAccessToken = getStringFromEnvironment(env,
			"registration_client_endpoint_response",
			"body_json.registration_access_token",
			"registration_access_token in client configuration endpoint response");

		if (!registrationClientUri.equals(expectedRegistrationClientUri)) {
			throw error("registration_client_uri in client configuration response does not match the one in the initial registration response",
				args("registration_client_uri", registrationClientUri, "expected", expectedRegistrationClientUri));
		}

		if (!registrationAccessToken.equals(expectedRegistrationAccessToken)) {
			throw error("registration_access_token in client configuration response does not match the one in the initial registration response",
				args("registration_access_token", registrationAccessToken, "expected", expectedRegistrationAccessToken));
		}

        logSuccess("Client configuration endpoint returned correct registration_access_token and registration_client_uri.");

        return env;
    }
}
