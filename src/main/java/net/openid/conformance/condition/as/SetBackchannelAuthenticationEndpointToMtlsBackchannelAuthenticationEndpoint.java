package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetBackchannelAuthenticationEndpointToMtlsBackchannelAuthenticationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		env.putString("server", "backchannel_authentication_endpoint", env.getString("server", "mtls_endpoint_aliases.backchannel_authentication_endpoint"));

		log("Set backchannel authentication endpoint to be the MTLS backchannel authentication endpoint");

		return env;
	}

}
