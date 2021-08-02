package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ExtractDynamicRegistrationResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_endpoint_response")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		JsonObject client = (JsonObject) env.getElementFromObject("dynamic_registration_endpoint_response", "body_json");

		env.putObject("client", client);

		if (!client.has("registration_client_uri") &&
			!client.has("registration_access_token")) {

			log("Dynamic registration returned neither registration_client_uri nor registration_access_token");
			return env;
		}

		if (!client.has("registration_client_uri")) {
			throw error("Dynamic registration returned registration_access_token but not registration_client_uri");
		}
		if (!client.has("registration_access_token")) {
			throw error("Dynamic registration returned registration_client_uri but not registration_access_token");
		}

		String registrationClientUri = OIDFJSON.getString(client.get("registration_client_uri"));
		String registrationAccessToken = OIDFJSON.getString(client.get("registration_access_token"));

		if (Strings.isNullOrEmpty(registrationClientUri)) {
			throw error("registration_client_uri must not be an empty string");
		}
		if (Strings.isNullOrEmpty(registrationAccessToken)) {
			throw error("registration_access_token must not be an empty string");
		}

		env.putString("registration_client_uri", registrationClientUri);
		env.putString("registration_access_token", registrationAccessToken);

		logSuccess("Extracted dynamic registration management credentials",
			args("registration_client_uri", registrationClientUri,
				"registration_access_token", registrationAccessToken));

		return env;
	}

}
