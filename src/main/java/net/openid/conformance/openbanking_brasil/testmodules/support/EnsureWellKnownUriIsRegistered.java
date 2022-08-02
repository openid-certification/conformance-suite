package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;

public class EnsureWellKnownUriIsRegistered extends AbstractCondition {
	@Override
	@PreEnvironment(required = {"directory_participants_response_full", "config"})
	public Environment evaluate(Environment env) {
		String participantsArrayJsonString = env.getString("directory_participants_response_full", "body");

		Gson gson = JsonUtils.createBigDecimalAwareGson();
		JsonArray participantsArray = gson.fromJson(participantsArrayJsonString, JsonArray.class);

		String configWellKnownUri = env.getString("config", "server.discoveryUrl");

		for (JsonElement org : participantsArray) {
			JsonObject orgObject = org.getAsJsonObject();
			if (orgObject.has("AuthorisationServers")) {
				JsonArray servers = orgObject.get("AuthorisationServers").getAsJsonArray();
				for (JsonElement server : servers) {
					JsonObject serverObject = server.getAsJsonObject();
					if(serverObject.has("OpenIDDiscoveryDocument")){
						String registeredWellKnownUri = OIDFJSON.getString(serverObject.get("OpenIDDiscoveryDocument"));
						if(registeredWellKnownUri.equals(configWellKnownUri)){
							logSuccess("Provided Well Known URI is registered in the Directory",
								args("Well-Known", configWellKnownUri, "AS", serverObject));
							return env;
						}
					}else {
						throw error("Could not find OpenIDDiscoveryDocument JSON element in the AuthorisationServer object",
							args("AS", serverObject));
					}
				}
			}else {
				throw error("Could not find AuthorisationServers JSON array in the organisation object",
					args("Organisation", orgObject));
			}
		}
		throw error("Could not find Authorisation Server with provided Well-Known URL in the Directory Participants List",
			args("Well-Known", configWellKnownUri));
	}
}
