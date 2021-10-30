package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class CreateClientConfigurationRequestFromDynamicClientRegistrationResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_endpoint_response" })
	@PostEnvironment(required = "registration_client_endpoint_request_body")
	public Environment evaluate(Environment env) {

		JsonObject request = (JsonObject) env.getElementFromObject("dynamic_registration_endpoint_response", "body_json").deepCopy();

		// as per https://datatracker.ietf.org/doc/html/rfc7592#section-2.2 these 4 must not be included
		for (String s : List.of("registration_access_token",
			"registration_client_uri",
			"client_secret_expires_at",
			"client_id_issued_at")) {
			request.remove(s);
		}

		env.putObject("registration_client_endpoint_request_body", request);

		log("Created client configuration request body from dynamic client registration response",
			args("client_configuration_request", request));

		return env;
	}
}
