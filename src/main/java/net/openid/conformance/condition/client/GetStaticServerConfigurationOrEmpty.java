package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * For flows that need no server metadata from the tester (e.g. OpenID4VP over the Digital
 * Credentials API, where there is no authorization endpoint): as GetStaticServerConfiguration,
 * but uses an empty 'server' object when the test configuration has none.
 */
public class GetStaticServerConfigurationOrEmpty extends GetStaticServerConfiguration {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement server = env.getElementFromObject("config", "server");
		if (server == null || server.isJsonNull()) {
			env.putObject("server", new JsonObject());
			logSuccess("No server object in the test configuration; none is needed, so using an empty one");
			return env;
		}
		return super.evaluate(env);
	}

}
