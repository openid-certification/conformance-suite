package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractServerJWKsFromOPConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = { "op_server_jwks" })
	public Environment evaluate(Environment env) {

		JsonElement jwksElement = env.getElementFromObject("config", "federation.op_server_jwks");
		if (jwksElement == null) {
			throw error("The configuration does not contain the required op_server_jwks");
		}

		JsonObject jwks = jwksElement.getAsJsonObject();
		env.putObject("op_server_jwks", jwks);

		logSuccess("Extracted JWKs from config", args("op_server_jwks", jwks));

		return env;
	}

}
