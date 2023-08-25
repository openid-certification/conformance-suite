package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class StoreOriginalClient2Configuration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "original_client_config")
	public Environment evaluate(Environment env) {

		JsonElement dynamicClientRegistrationTemplate = env.getElementFromObject("config", "client2");
		if (dynamicClientRegistrationTemplate == null || !dynamicClientRegistrationTemplate.isJsonObject()) {
			// we don't actually need anything; if no client_name is given we'll pick one
			env.putObject("original_client_config", new JsonObject());
			log("No client details on configuration, created an empty original_client_config object.");
		} else {
			// we've got a client object, put it in the environment
			env.putObject("original_client_config", dynamicClientRegistrationTemplate.getAsJsonObject());

			log("Created original_client_config object from the client configuration.", dynamicClientRegistrationTemplate.getAsJsonObject());
		}
		return env;
	}
}
