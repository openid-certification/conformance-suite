package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VCISelectOAuthorizationServer extends VCIFetchOAuthorizationServerMetadata {

	@PreEnvironment(required = {"vci"})
	@Override
	public Environment evaluate(Environment env) {

		JsonElement credentialIssuerMetadataEl = env.getElementFromObject("vci", "credential_issuer_metadata");
		JsonObject credentialIssuerMetadata = credentialIssuerMetadataEl.getAsJsonObject();

		String credentialIssuerIssuer = OIDFJSON.getString(credentialIssuerMetadata.get("credential_issuer"));
		String forcedAuthorizationServer = env.getString("config", "vci.authorization_server");

		JsonElement authorizationServersEL = credentialIssuerMetadata.get("authorization_servers");
		JsonArray authorizationServerArray;
		if (authorizationServersEL == null) {
			String credentialIssuer = OIDFJSON.getString(credentialIssuerMetadata.get("credential_issuer"));
			log("Derived authorization server metadata endpoint URL from credential issuer.", args("credential_issuer", credentialIssuer));
			authorizationServerArray = new JsonArray();
			authorizationServerArray.add(credentialIssuer);
		} else {
			if (!authorizationServersEL.isJsonArray()) {
				throw error("Expected authorization_servers field to be an array", args("authorization_servers", authorizationServersEL));
			}

			authorizationServerArray = authorizationServersEL.getAsJsonArray();
		}

		String wantedAuthorizationServerIssuer = forcedAuthorizationServer;
		if (wantedAuthorizationServerIssuer == null) {
			wantedAuthorizationServerIssuer = credentialIssuerIssuer;
		}

		int index;
		if (wantedAuthorizationServerIssuer == null) {
			if (authorizationServerArray.size() > 1) {
				throw error("More than one authorization server listed in 'authorization_servers' - please specify the one to test in the 'Authorization Server URL' field in the test configuration.",
					args("authorization_servers", authorizationServerArray));
			}
			index = 0;
		} else {
			index = authorizationServerArray.asList().indexOf(new JsonPrimitive(wantedAuthorizationServerIssuer));
			if (index < 0) {
				throw error("Authorization server listed in 'Authorization Server URL' or  in the test configuration is not listed in the 'authorization_servers' array in the credential issuer metadata.",
					args("test_config_issuer_url", credentialIssuerIssuer, "authorization_server_issuer_url", wantedAuthorizationServerIssuer, "authorization_servers", authorizationServerArray));
			}
		}

		JsonElement serverData = env.getElementFromObject("vci", "authorization_servers.server" + index + ".authorization_server_metadata");
		if (serverData == null) {
			throw error("No authorization server metadata found for index " + index);
		}
		env.putObject("server", serverData.getAsJsonObject());

		logSuccess("Select authorization server metadata", args("index", index, "selected_authorization_server", wantedAuthorizationServerIssuer, "authorization_server_metadata", serverData, "available_authorization_servers", authorizationServerArray));

		return env;
	}


}
