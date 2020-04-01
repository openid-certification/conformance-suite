package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class OIDCCGenerateServerConfigurationWithRefreshTokenGrantType extends OIDCCGenerateServerConfiguration {

	@Override
	protected void addGrantTypes(JsonObject server) {
		super.addGrantTypes(server);
		JsonArray grantTypes = server.getAsJsonArray("grant_types_supported");
		grantTypes.add("refresh_token");
	}
}
