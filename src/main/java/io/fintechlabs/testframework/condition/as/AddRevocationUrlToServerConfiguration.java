package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddRevocationUrlToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server", strings = "base_url")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		String baseUrl = env.getString("base_url");

		// set off the URLs below with a slash, if needed
		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}

		JsonObject server = env.getObject("server");

		server.addProperty("revocation_endpoint", baseUrl + "revoke");

		return env;
	}

}
