package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;
import java.util.Map;

public class AddSupportedOpenIdScopesToClientConfig extends AbstractCondition {
	private static final List<String> OPEN_BANKING_SCOPES = List.of("openid", "payments", "accounts", "credit-cards-accounts", "consents",
		"customers", "invoice-financings", "financings", "loans", "unarranged-accounts-overdraft", "resources");

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement scopesSupported = env.getElementFromObject("server", "scopes_supported");

		String supportedOpenBankingScopes = "";
		for (JsonElement scope : scopesSupported.getAsJsonArray()) {
			String supportedScope = OIDFJSON.getString(scope);
			if (OPEN_BANKING_SCOPES.contains(supportedScope)) {
				if (!supportedOpenBankingScopes.isEmpty()) {
					supportedOpenBankingScopes += " " + supportedScope;
				}else {
					supportedOpenBankingScopes = supportedScope;
				}
			}
		}

		if (!supportedOpenBankingScopes.isEmpty()) {
			env.putString("original_client_config", "scope", supportedOpenBankingScopes);
			env.putString("config", "client.scope", supportedOpenBankingScopes);
			log("Added supported open banking scopes to the client config", Map.of("Scopes", supportedOpenBankingScopes));
		} else {
			throw error("Server does not support open banking scopes");
		}

		return env;
	}
}
