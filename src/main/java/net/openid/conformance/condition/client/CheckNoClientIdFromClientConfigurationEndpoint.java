package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckNoClientIdFromClientConfigurationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "registration_client_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonElement clientId = env.getElementFromObject("registration_client_endpoint_response", "body_json.client_id");

		if (clientId != null) {
			throw error("'client_id' field found in response from client configuration endpoint, but the request was expected to fail.",
				args("client_id", clientId));
		}

		logSuccess("Client configuration endpoint did not return a client_id.");

		return env;
	}
}
