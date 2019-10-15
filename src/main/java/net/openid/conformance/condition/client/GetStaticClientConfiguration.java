package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GetStaticClientConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "client", strings = "client_id")
	public Environment evaluate(Environment env) {
		// make sure we've got a client object
		JsonElement client = env.getElementFromObject("config", "client");
		if (client == null || !client.isJsonObject()) {
			throw error("Definition for client not present in supplied configuration");
		} else {
			// we've got a client object, put it in the environment
			env.putObject("client", client.getAsJsonObject());

			// pull out the client ID and put it in the root environment for easy access
			env.putString("client_id", env.getString("client", "client_id"));

			logSuccess("Found a static client object", client.getAsJsonObject());
			return env;
		}
	}

}
