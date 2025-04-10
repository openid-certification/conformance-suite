package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientIdInQueryStringMatchesEntityIdentifier extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_http_request", "client"})
	public Environment evaluate(Environment env) {

		String queryStringClientId = env.getString("authorization_endpoint_http_request", "query_string_params.client_id");
		String configuredClientId = env.getString("client", "client_id");

		if (queryStringClientId == null || !queryStringClientId.equals(configuredClientId)) {
			throw error("Client_id in query string does not match the configured client_id/entity_identifier",
				args("query_string_client_id", queryStringClientId, "configured_client_id", configuredClientId));
		}

		logSuccess("Client_id in query string matches the configured client_id/entity_identifier");
		return env;
	}

}
