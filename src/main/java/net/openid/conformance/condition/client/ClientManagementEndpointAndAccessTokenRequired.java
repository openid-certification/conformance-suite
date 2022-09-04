package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ClientManagementEndpointAndAccessTokenRequired extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String registrationClientUri = env.getString("client", "registration_client_uri");
		String registrationAccessToken = env.getString("client", "registration_access_token");

		if (registrationClientUri == null ||
			registrationAccessToken == null) {
			throw error("The authorization server did not return a client management endpoint and access token.");
		}

		// VerifyClientManagementCredentials already checked they're non null, so nothing further to do here.

		logSuccess("Client management endpoint and access token were provided by the authorization server.",
			args("registration_client_uri", registrationClientUri,
				"registration_access_token", registrationAccessToken));

		return env;
	}

}
