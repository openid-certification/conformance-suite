package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Points subsequent token endpoint calls at the token_endpoint from the root of the server's discovery
 * document, instead of the mtls_endpoint_aliases entry AddMTLSEndpointAliasesToEnvironment placed there.
 */
public class SetTokenEndpointToNonMtlsTokenEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(strings = "token_endpoint")
	public Environment evaluate(Environment env) {
		String tokenEndpoint = env.getString("server", "token_endpoint");
		if (tokenEndpoint == null) {
			throw error("token_endpoint missing from server configuration");
		}

		env.putString("token_endpoint", tokenEndpoint);

		logSuccess("Set token endpoint to the token_endpoint outside mtls_endpoint_aliases",
			args("token_endpoint", tokenEndpoint,
				"mtls_token_endpoint", env.getString("server", "mtls_endpoint_aliases.token_endpoint")));

		return env;
	}

}
