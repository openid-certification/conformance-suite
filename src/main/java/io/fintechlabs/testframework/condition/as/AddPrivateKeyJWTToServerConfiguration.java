package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddPrivateKeyJWTToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonArray data = new JsonArray();
		data.add("private_key_jwt");

		JsonObject server = env.getObject("server");
		server.add("token_endpoint_auth_methods_supported", data);

		logSuccess("Added private_key_jwt for token_endpoint_auth_methods_supported");

		return env;
	}
}
