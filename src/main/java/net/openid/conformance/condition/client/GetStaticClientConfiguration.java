package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class GetStaticClientConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "client", strings = "client_id")
	public Environment evaluate(Environment env) {
		// make sure we've got a client object
		JsonElement clientEl = env.getElementFromObject("config", "client");
		if (clientEl == null || !clientEl.isJsonObject()) {
			throw error("As static client was selected, the test configuration must contain a client configuration");
		} else {
			JsonObject client = clientEl.getAsJsonObject();
			// we've got a client object, put it in the environment
			env.putObject("client", client);

			JsonElement clientId = client.get("client_id");
			if (clientId == null) {
				throw error("As static client was selected, the test configuration must contain a client_id");
			}
			if (!clientId.isJsonPrimitive() || !clientId.getAsJsonPrimitive().isString()) {
				throw error("client_id in test configuration is not a string");
			}

			// pull out the client ID and put it in the root environment for easy access
			env.putString("client_id", OIDFJSON.getString(clientId));

			logSuccess("Found a static client object", client);
			return env;
		}
	}

}
