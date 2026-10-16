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
			throw error("'PDP JWK Set' field is empty or missing from the 'AuthZEN' section in the test configuration (required to verify the discovery metadata 'signed_metadata' JWT signature)");
		}
		JsonElement keys = jwks.getAsJsonObject().get("keys");
		if (keys == null || !keys.isJsonArray()) {
			throw error("'PDP JWK Set' in the 'AuthZEN' section of the test configuration must be a JWK Set, "
				+ "{\"keys\": [ ... ]}, not a single JWK. Put the key inside a \"keys\" array.",
				args("pdp_jwks", jwks));
		}
		logSuccess("'PDP JWK Set' is configured in the test configuration");
		return env;
	}
}
