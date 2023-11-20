package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckClientConfigurationAccessTokenFromClientConfigurationEndpointIsNotRotated extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "registration_client_endpoint_response", "client" })
	public Environment evaluate(Environment env) {

		String originalRegistrationAccessToken = env.getString("client", "registration_access_token");

		String registrationAccessToken = getStringFromEnvironment(env,
			"registration_client_endpoint_response",
			"body_json.registration_access_token",
			"registration_access_token in client configuration endpoint response");

		if (!registrationAccessToken.equals(originalRegistrationAccessToken)) {
			env.putString("client", "registration_access_token", registrationAccessToken);
			throw error("Client configuration endpoint returned new registration_access_token.",
				args("original", originalRegistrationAccessToken, "new", registrationAccessToken));
		}

		logSuccess("Client configuration endpoint returned same registration_access_token as previously.");

		return env;
	}
}
