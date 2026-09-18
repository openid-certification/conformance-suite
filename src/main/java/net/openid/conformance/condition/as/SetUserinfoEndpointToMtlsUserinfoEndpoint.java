package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetUserinfoEndpointToMtlsUserinfoEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		env.putString("server", "userinfo_endpoint", env.getString("server", "mtls_endpoint_aliases.userinfo_endpoint"));

		log("Set userinfo endpoint to be the MTLS userinfo endpoint");

		return env;
	}

}
