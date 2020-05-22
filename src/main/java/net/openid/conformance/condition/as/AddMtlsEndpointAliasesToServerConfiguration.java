package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.runner.TestDispatcher;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddMtlsEndpointAliasesToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		String tokenEndpoint = OIDFJSON.getString(server.get("token_endpoint"));
		String mtlsTokenEndpoint = tokenEndpoint.replaceFirst(TestDispatcher.TEST_PATH, TestDispatcher.TEST_MTLS_PATH);
		JsonObject aliases = new JsonObject();
		aliases.addProperty("token_endpoint", mtlsTokenEndpoint);

		server.add("mtls_endpoint_aliases", aliases);

		log("Added mtls_endpoint_aliases to server configuration", args("mtls_endpoint_aliases", aliases));

		return env;
	}
}
