package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetParEndpointToMtlsParEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {
		env.putString("server", "pushed_authorization_request_endpoint", env.getString("server", "mtls_endpoint_aliases.pushed_authorization_request_endpoint"));

		log("Set PAR endpoint to be the MTLS PAR endpoint");
		return env;
	}

}
