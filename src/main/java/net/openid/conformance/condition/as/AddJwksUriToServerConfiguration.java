package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddJwksUriToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}
		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}

		JsonObject server = env.getObject("server");
		String uri = baseUrl + "jwks";
		server.addProperty("jwks_uri", uri);

		log("Added jwks_uri to server configuration", args ("jwks_uri", uri));

		return env;
	}
}
