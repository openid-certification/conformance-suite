package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientIdInParametersMatchesEntityIdentifier extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_http_request", "client"})
	public Environment evaluate(Environment env) {

		String clientId = null;
		if ("GET".equals(env.getString("authorization_endpoint_http_request", "method"))) {
			clientId = env.getString("authorization_endpoint_http_request", "query_string_params.client_id");
		} else if ("POST".equals(env.getString("authorization_endpoint_http_request", "method"))) {
			clientId = env.getString("authorization_endpoint_http_request", "body_form_params.client_id");
		}

		String configuredClientId = env.getString("client", "client_id");

		if (clientId == null || !clientId.equals(configuredClientId)) {
			throw error("Client_id in parameters does not match the configured client_id/entity_identifier",
				args("client_id", clientId, "configured_client_id", configuredClientId));
		}

		logSuccess("Client_id in parameters matches the configured client_id/entity_identifier");
		return env;
	}

}
