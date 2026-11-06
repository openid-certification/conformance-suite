package net.openid.conformance.vci10issuer.condition;

import com.google.common.base.Strings;
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
		String credentialIssuerMetadataUrl = env.getString("vci", "credential_issuer_metadata_url");

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
			if (authorizationServerArray.isEmpty()) {
				throw error("The 'authorization_servers' array in the credential issuer metadata is empty, but when present it must list at least one authorization server.",
					args("authorization_servers", authorizationServerArray, "credential_issuer_metadata_url", credentialIssuerMetadataUrl));
			}
		}

		int index;
		String wantedAuthorizationServerIssuer;
		// an empty string, which older saved configurations and ones edited as JSON can hold, means not configured
		if (!Strings.isNullOrEmpty(forcedAuthorizationServer)) {
			wantedAuthorizationServerIssuer = forcedAuthorizationServer;
			index = authorizationServerArray.asList().indexOf(new JsonPrimitive(wantedAuthorizationServerIssuer));
			if (index < 0) {
				throw error("The 'Authorization Server URL Override' in the 'Credential Issuer' section of the test configuration is not listed in the 'authorization_servers' array in the credential issuer metadata.",
					args("authorization_server_issuer_url", wantedAuthorizationServerIssuer, "authorization_servers", authorizationServerArray, "credential_issuer_metadata_url", credentialIssuerMetadataUrl));
			}
		} else if (authorizationServerArray.size() == 1) {
			index = 0;
			wantedAuthorizationServerIssuer = OIDFJSON.getString(authorizationServerArray.get(0));
		} else {
			// with several authorization servers, the credential issuer acting as its own is the natural default
			wantedAuthorizationServerIssuer = credentialIssuerIssuer;
			index = authorizationServerArray.asList().indexOf(new JsonPrimitive(wantedAuthorizationServerIssuer));
			if (index < 0) {
				throw error("More than one authorization server is listed in 'authorization_servers' in the credential issuer metadata - please specify the one to test in the 'Authorization Server URL Override' field in the test configuration.",
					args("credential_issuer", credentialIssuerIssuer, "authorization_servers", authorizationServerArray, "credential_issuer_metadata_url", credentialIssuerMetadataUrl));
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
