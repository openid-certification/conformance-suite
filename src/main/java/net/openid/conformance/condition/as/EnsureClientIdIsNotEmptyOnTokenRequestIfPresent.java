package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * RFC 6749 requires an authorization server to treat a parameter sent without a value as if it were
 * omitted, but servers that do not may reject an empty client_id. Callers should use WARNING.
 */
public class EnsureClientIdIsNotEmptyOnTokenRequestIfPresent extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	public Environment evaluate(Environment env) {

		String clientId = env.getString("token_endpoint_request", "body_form_params.client_id");

		if (clientId == null) {
			log("client_id not present, nothing to check");
			return env;
		}

		if (clientId.isEmpty()) {
			throw error("The client sent a client_id parameter with an empty value. The authorization server must " +
				"treat it as if it were omitted, but some may reject the request. The client should either omit " +
				"client_id or send its client_id.");
		}

		logSuccess("client_id is not empty", args("client_id", clientId));
		return env;
	}
}
