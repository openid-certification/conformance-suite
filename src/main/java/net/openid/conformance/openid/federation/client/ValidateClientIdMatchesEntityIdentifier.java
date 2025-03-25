package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientIdMatchesEntityIdentifier extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String requestObjectClientId = env.getString("request_object_client_id");
		String configuredClientId = env.getString("client", "client_id");

		if (requestObjectClientId == null || !requestObjectClientId.equals(configuredClientId)) {
			throw error("Client_id in request object does not match the configured client_id/entity_identifier",
				args("request_object_client_id", requestObjectClientId, "configured_client_id", configuredClientId));
		}

		logSuccess("Client_id in request object matches the configured client_id/entity_identifier");
		return env;
	}

}
