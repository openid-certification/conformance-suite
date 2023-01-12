package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "registration_client_endpoint_response", "client" })
	public Environment evaluate(Environment env) {

		String originalRegistrationAccessToken = env.getString("client", "registration_access_token");

		String registrationAccessToken = getStringFromEnvironment(env,
			"registration_client_endpoint_response",
			"body_json.registration_access_token",
			"registration_access_token in client configuration endpoint response");

		if (!registrationAccessToken.equals(originalRegistrationAccessToken)) {
			// access token has been rotated; this probably isn't recommended but it is allowed (at least for now)
			// as per https://github.com/OpenBanking-Brasil/specs-seguranca/issues/198
			env.putString("client", "registration_access_token", registrationAccessToken);
			logSuccess("Client configuration endpoint returned new registration_access_token.",
				args("original", originalRegistrationAccessToken, "new", registrationAccessToken));
			return env;
		}

		logSuccess("Client configuration endpoint returned same registration_access_token as previously.");

		return env;
	}
}
