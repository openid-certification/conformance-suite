package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonArray data = new JsonArray();
		data.add("private_key_jwt");

		JsonObject server = env.getObject("server");
		server.add("token_endpoint_auth_methods_supported", data);

		log("Added private_key_jwt to token_endpoint_auth_methods_supported");

		return env;
	}
}
