package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class RemoveConsentIdFromClientScopes extends AbstractCondition {
	@Override
	@PreEnvironment(strings = "consent_id", required = "client")
	public Environment evaluate(Environment env) {
		String scopeToBeRemoved = " consent:" + env.getString("consent_id");
		JsonObject client = env.getObject("client");
		JsonElement scopeElement = client.get("scope");
		if (scopeElement == null) {
			throw error("Scope element is missing in client");
		}
		String clientScopes = OIDFJSON.getString(scopeElement);

		if (clientScopes.contains(scopeToBeRemoved)) {
			clientScopes = clientScopes.replace(scopeToBeRemoved, "");
			client.addProperty("scope", clientScopes);
			logSuccess("Consent ID was removed from client scope", Map.of("Removed ID", scopeToBeRemoved, "Client", client));
		}else {
			log("Client scopes does not contain the consent ID. Skipping");
		}

		return env;
	}
}
