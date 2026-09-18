package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetTokenEndpointToMtlsTokenEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		env.putString("server", "token_endpoint", env.getString("server", "mtls_endpoint_aliases.token_endpoint"));

		log("Set token endpoint to be the MTLS token endpoint");

		return env;
	}

}
