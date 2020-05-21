package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.runner.TestDispatcher;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class SetTokenEndpointAuthMethodsSupportedToTlsClientAuthOnly extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonArray data = new JsonArray();
		data.add("tls_client_auth");

		JsonObject server = env.getObject("server");
		server.add("token_endpoint_auth_methods_supported", data);

		String tokenEndpoint = OIDFJSON.getString(server.get("token_endpoint"));
		tokenEndpoint = tokenEndpoint.replaceFirst(TestDispatcher.TEST_PATH, TestDispatcher.TEST_MTLS_PATH);
		server.addProperty("token_endpoint", tokenEndpoint);

		log("Set token_endpoint_auth_methods_supported to tls_client_auth only and replaced token_endpoint with the MTLS one");


		return env;
	}
}
