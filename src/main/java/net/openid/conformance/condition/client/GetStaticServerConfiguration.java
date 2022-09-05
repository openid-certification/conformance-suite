package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GetStaticServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		String discoveryUrl = env.getString("config", "server.discoveryUrl");
		String iss = env.getString("config", "server.discoveryIssuer");

		if (!Strings.isNullOrEmpty(discoveryUrl) || !Strings.isNullOrEmpty(iss)) {
			throw error("Test set to use static server configuration but test configuration contains discovery information", args("discoveryUrl", discoveryUrl, "discoveryIssuer", iss));
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
