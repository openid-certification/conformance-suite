package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;


public class EnsurePDPJwksConfigured extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config"})
	public Environment evaluate(Environment env) {
		JsonElement jwks = env.getElementFromObject("config", "pdp.jwks");
		if((jwks==null) || !jwks.isJsonObject() || jwks.getAsJsonObject().isEmpty()) {
			throw error("'PDP JWK Set' field is empty or missing from the 'PDP' section in the test configuration (required to verify the discovery metadata 'signed_metadata' JWT signature)");
		}
		logSuccess("'PDP JWK Set' is configured in the test configuration");
		return env;
	}
}
