package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

public class OIDCCCheckScopesSupportedContainScopeTest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server", "client"})
	public Environment evaluate(Environment env) {

		JsonElement scopesSupported = env.getElementFromObject("server", "scopes_supported");
		String expectedScopeStr = env.getString("client", "scope");
		String errorMessage = null;

		if (scopesSupported == null) {
			errorMessage = "'scopes_support' is missing from discovery document";
		} else if(!scopesSupported.isJsonArray()) {
			errorMessage = "'scopes_support' in discovery document is not a array";
		} else {

			// convert JsonArray scopesSupported to list string
			List<String> scopesSupportedList = new ArrayList<>();
			for (JsonElement scope : scopesSupported.getAsJsonArray()) {
				scopesSupportedList.add(OIDFJSON.getString(scope));
			}

			String [] expectedScopes = expectedScopeStr.split(" ");
			for (String expectedScope : expectedScopes) {
				if (!scopesSupportedList.contains(expectedScope)) {
					errorMessage = "'scopes_support' in discovery document doesn't contain expected scopes";
				}
			}
		}

		if (errorMessage != null) {
			// skip test when scopes is not supported
			env.putBoolean("scopes_not_supported_flag", true);
			throw error(errorMessage, args("expected", expectedScopeStr, "actual", scopesSupported));
		}

		logSuccess("'scopes_supported' in discovery document contain expected scopes", args("expected", expectedScopeStr, "actual", scopesSupported));

		return env;
	}

}
