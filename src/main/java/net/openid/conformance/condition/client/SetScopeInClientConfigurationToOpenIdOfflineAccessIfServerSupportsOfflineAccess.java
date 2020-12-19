package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

public class SetScopeInClientConfigurationToOpenIdOfflineAccessIfServerSupportsOfflineAccess extends AbstractSetScopeInClientConfiguration {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JsonElement scopesSupported = env.getElementFromObject("server", "scopes_supported");

		if (scopesSupported == null) {
			log("scopes_supported is not present in the discovery document, so assuming server does not support 'offline_access' scope and hence not adding it to the list of scopes to be requested");
			return env;
		}

		if (!scopesSupported.isJsonArray()) {
			throw error("'scopes_supported' is not a array");
		}
		// convert JsonArray scopesSupported to list string
		List<String> scopesSupportedList = new ArrayList<>();
		for (JsonElement scope : scopesSupported.getAsJsonArray()) {
			scopesSupportedList.add(OIDFJSON.getString(scope));
		}

		if (!scopesSupportedList.contains("offline_access")) {
			log("scopes supported does not contain 'offline_access' so not adding it to the list of scopes to be requested",
				args("scopes_supported", scopesSupportedList));
			return env;
		}
		return setScopeInClientConfiguration(env, "openid offline_access", "as 'scope_supported' contains 'offline_access'");
	}

}
