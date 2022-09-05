package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureServerConfigurationSupportsRefreshToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement supportedGrantTypesElement = env.getElementFromObject("server", "grant_types_supported");

		if (supportedGrantTypesElement == null) {
			// Null implies default ["authorization_code", "implicit"]
			throw error("The server issued a refresh token but does not claim to support this grant type (grant_types_supported in not present in the discovery document)");
		}

		JsonArray supportedGrantTypes;

		try {
			supportedGrantTypes = supportedGrantTypesElement.getAsJsonArray();
		} catch (IllegalStateException e) {
			throw error("supported_grant_types is present in the discovery document but is not an array");
		}

		for (JsonElement grantType : supportedGrantTypes) {
			if (OIDFJSON.getString(grantType).equals("refresh_token")) {
				logSuccess("The server configuration indicates support for refresh tokens", args("supported_grant_types", supportedGrantTypes));
				return env;
			}
		}

		throw error("The server issued a refresh token but does not claim to support this grant type", args("supported_grant_types", supportedGrantTypes));
	}
}
