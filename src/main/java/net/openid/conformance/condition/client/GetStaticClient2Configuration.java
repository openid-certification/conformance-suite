package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GetStaticClient2Configuration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "client2")
	public Environment evaluate(Environment env) {
		if (!env.containsObject("config")) {
			throw error("Couldn't find a configuration");
		}

		// make sure we've got a client object
		JsonElement client = env.getElementFromObject("config", "client2");
		if (client == null || !client.isJsonObject()) {
			throw error("Definition for client2 not present in supplied configuration");
		} else {
			// we've got a client object, put it in the environment
			env.putObject("client2", client.getAsJsonObject());

			logSuccess("Found a static second client object", client.getAsJsonObject());
			return env;
		}
	}

}
