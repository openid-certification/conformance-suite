package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public class EnsureServerConfigurationDoesNotSupportRefreshToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement supportedGrantTypesElement = env.getElementFromObject("server", "grant_types_supported");

		if (supportedGrantTypesElement == null) {
			// Null implies default ["authorization_code", "implicit"]
			logSuccess("The server did not issue a refresh token and does not claim to support this grant type (grant_types_supported in not present in the discovery document)");
			return env;
		}

		JsonArray supportedGrantTypes;

		try {
			supportedGrantTypes = supportedGrantTypesElement.getAsJsonArray();
		} catch (IllegalStateException e) {
			throw error("supported_grant_types is present in the discovery document but is not an array");
		}

		for (JsonElement grantType : supportedGrantTypes) {
			if (OIDFJSON.getString(grantType).equals("refresh_token")) {
				throw error("The server supports refresh tokens, but did not issue one. This is acceptable if the server has a policy of issuing refresh tokens to some clients, but not to FAPI-RW clients. If the server requires scope=offline_access to issue a refresh token please add that to the scope listed in the test configuration.", args("supported_grant_types", supportedGrantTypes));
			}
		}

		logSuccess("The server did not issue a refresh token, and does not claim to support this grant type", args("supported_grant_types", supportedGrantTypes));
		return env;
	}
}
