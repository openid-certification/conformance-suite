package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckClientIdFromClientConfigurationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "registration_client_endpoint_response",  "client" })
	public Environment evaluate(Environment env) {
		String expectedClientId = getStringFromEnvironment(env,"client", "client_id", "client id in client config");

		String clientId = getStringFromEnvironment(env, "registration_client_endpoint_response", "body_json.client_id", "client id in client config response");

		if (!clientId.equals(expectedClientId)) {
			throw error("'client_id' field found in response from client configuration endpoint does not match expected client_id.",
				args("client_id", clientId, "expected", expectedClientId));
		}

		logSuccess("Client configuration endpoint returned correct client_id.");

		return env;
	}
}
