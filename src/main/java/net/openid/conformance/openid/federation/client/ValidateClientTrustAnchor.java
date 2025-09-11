package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.federation.ValidateUrlRequirements;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateClientTrustAnchor extends ValidateUrlRequirements {

	@Override
	@PreEnvironment(required = { "config" } )
	public Environment evaluate(Environment env) {
		String label = "Client trust anchor";
		String fieldName = "trust_anchor";
		JsonElement element = env.getElementFromObject("config", "client.trust_anchor");
		if (element == null) {
			throw error("No client trust anchor in configuration");
		}

		String clientTrustAnchor = OIDFJSON.getString(element);
		if (clientTrustAnchor.equals(env.getString("config", "federation.entity_identifier"))) {
			throw error("Configuration error: Client trust anchor cannot be the same as the client entity identifier");
		}

		return validateUrlRequirements(element, fieldName, label, env);
	}

}
