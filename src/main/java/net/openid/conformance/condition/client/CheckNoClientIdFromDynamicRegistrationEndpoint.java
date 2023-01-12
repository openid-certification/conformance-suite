package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckNoClientIdFromDynamicRegistrationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonElement clientId = env.getElementFromObject("dynamic_registration_endpoint_response", "body_json.client_id");

		if (clientId != null) {
			throw error("'client_id' field found in response from dynamic registration response, but the request was expected to fail.",
				args("client_id", clientId));
		}

		logSuccess("Dynamic registration response did not include a client_id.");

		return env;
	}
}
