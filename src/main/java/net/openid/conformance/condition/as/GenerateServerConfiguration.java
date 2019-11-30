package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GenerateServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(required = "server", strings = { "issuer", "discoveryUrl" })
	public Environment evaluate(Environment env) {

		String baseUrl = env.getString("base_url");

		if (Strings.isNullOrEmpty(baseUrl)) {
			throw error("Couldn't find a base URL");
		}

		// set off the URLs below with a slash, if needed
		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}

		JsonObject server = createBaseConfiguration(env, baseUrl);

		logSuccess("Created server configuration", args("server", server, "issuer", baseUrl,
							"discoveryUrl", server.get("discoveryUrl").getAsString()));

		return env;

	}

	protected JsonObject createBaseConfiguration(Environment env, String baseUrl) {
		// create a base server configuration object based on the base URL
		JsonObject server = new JsonObject();

		server.addProperty("issuer", baseUrl);
		server.addProperty("authorization_endpoint", baseUrl + "authorize");
		server.addProperty("token_endpoint", baseUrl + "token");
		server.addProperty("jwks_uri", baseUrl + "jwks");
		// add this as the server configuration
		env.putObject("server", server);

		env.putString("issuer", baseUrl);
		env.putString("discoveryUrl", baseUrl + ".well-known/openid-configuration");
		return server;
	}
}
