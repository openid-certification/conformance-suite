package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class GetStaticServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("config")) {
			throw error("Couldn't find a configuration");
		}

		String discoveryUrl = env.getString("config", "server.discoveryUrl");
		String iss = env.getString("config", "server.discoveryIssuer");

		if (!Strings.isNullOrEmpty(discoveryUrl) || !Strings.isNullOrEmpty(iss)) {
			throw error("Dynamic configuration elements found, skipping static configuration", args("discoveryUrl", discoveryUrl, "discoveryIssuer", iss));
		}

		// make sure we've got a server object
		JsonElement server = env.getElementFromObject("config", "server");
		if (server == null || !server.isJsonObject()) {
			throw error("Couldn't find server object in configuration");
		} else {
			// we've got a server object, put it in the environment
			env.putObject("server", server.getAsJsonObject());

			logSuccess("Found a static server object", server.getAsJsonObject());
			return env;
		}
	}

}
