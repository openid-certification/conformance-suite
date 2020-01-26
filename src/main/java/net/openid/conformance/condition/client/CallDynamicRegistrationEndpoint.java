package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CallDynamicRegistrationEndpoint extends AbstractCallDynamicRegistrationEndpoint {

	@Override
	@PreEnvironment(required = {"server", "dynamic_registration_request"})
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		return callDynamicRegistrationEndpoint(env);
	}

	@Override
	protected Environment onRegistrationEndpointResponse(Environment env, JsonObject client) {

		env.putObject("client", client);

		if (client.has("registration_client_uri") &&
			client.has("registration_access_token")) {

			String registrationClientUri = OIDFJSON.getString(client.get("registration_client_uri"));
			String registrationAccessToken = OIDFJSON.getString(client.get("registration_access_token"));

			if (!Strings.isNullOrEmpty(registrationClientUri) &&
				!Strings.isNullOrEmpty(registrationAccessToken)) {
				env.putString("registration_client_uri", registrationClientUri);
				env.putString("registration_access_token", registrationAccessToken);

				logSuccess("Extracted dynamic registration management credentials",
					args("registration_client_uri", registrationClientUri,
						"registration_access_token", registrationAccessToken));
			}
		}
		return env;
	}

	@Override
	protected Environment onRegistrationEndpointError(Environment env, Throwable e, int code, String status, String body) {

		throw error("Error from the dynamic registration endpoint", e, args("code", code, "status", status, "body", body));
	}
}
