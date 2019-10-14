package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddTokenEndpointSigningAlg extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server", strings = "base_url")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonArray data = new JsonArray();
		data.add("PS256");

		JsonObject server = env.getObject("server");
		server.add("token_endpoint_auth_signing_alg", data);

		logSuccess("Set 'PS256' for token_endpoint_auth_signing_alg");

		return env;
	}
}
