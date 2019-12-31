package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDCCCheckScopesSupportedContainScopeTest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server", "expected_scopes"})
	public Environment evaluate(Environment env) {

		JsonElement scopesSupported = env.getElementFromObject("server", "scopes_supported");
		JsonArray expectedScopes = env.getElementFromObject("expected_scopes", "expected_scopes").getAsJsonArray();
		String errorMessage = null;

		if (scopesSupported == null) {
			errorMessage = "'scope_supported' is null";
		} else if(!scopesSupported.isJsonArray()) {
			errorMessage = "'scope_supported' is not a array";
		} else {
			for (JsonElement expectedScope : expectedScopes) {
				if (!scopesSupported.getAsJsonArray().contains(expectedScope)) {
					errorMessage = "'scope_supported' doesn't contain expected scopes";
				}
			}
		}

		if (errorMessage != null) {
			// skip test when scopes is not supported
			env.putBoolean("scopes_not_supported_flag", true);
			throw error(errorMessage, args("expected", expectedScopes, "actual", scopesSupported));
		}

		logSuccess("'scopes_supported' contain expected scopes", args("expected", expectedScopes, "actual", scopesSupported));

		return env;
	}

}
