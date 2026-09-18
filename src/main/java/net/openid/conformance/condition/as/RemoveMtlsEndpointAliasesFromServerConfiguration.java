package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveMtlsEndpointAliasesFromServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonObject server = env.getObject("server");
		server.remove("mtls_endpoint_aliases");

		log("Removed mtls_endpoint_aliases from server configuration");

		return env;
	}

}
