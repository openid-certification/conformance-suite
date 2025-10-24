package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractJWKsFromRPConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = { "rp_ec_jwks" })
	public Environment evaluate(Environment env) {

		JsonElement jwksElement = env.getElementFromObject("config", "federation.rp_ec_jwks");
		if (jwksElement == null) {
			throw error("The configuration does not contain the required rp_ec_jwks");
		}

		JsonObject jwks = jwksElement.getAsJsonObject();
		env.putObject("rp_ec_jwks", jwks);

		logSuccess("Extracted JWKs from config", args("rp_ec_jwks", jwks));

		return env;
	}

}
