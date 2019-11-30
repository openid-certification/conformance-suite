package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDCCGetStaticClientConfigurationForRPTests extends AbstractCondition {

	/**
	 * Converts a single redirect_uri string value to a redirect_uris array
	 * @param env
	 * @return
	 */
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
			JsonObject clientObject = client.getAsJsonObject();
			if(clientObject.has("redirect_uri")) {
				String redirectUri = clientObject.get("redirect_uri").getAsString();
				JsonArray redirectUrisArray = new JsonArray();
				redirectUrisArray.add(redirectUri);
				clientObject.remove("redirect_uri");
				clientObject.add("redirect_uris", redirectUrisArray);
			}
			env.putObject("client", clientObject);

			// pull out the client ID and put it in the root environment for easy access
			env.putString("client_id", env.getString("client", "client_id"));

			logSuccess("Found a static client object", client.getAsJsonObject());
			return env;
		}
	}

}
