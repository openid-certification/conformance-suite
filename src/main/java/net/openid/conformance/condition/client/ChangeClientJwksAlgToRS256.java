package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ChangeClientJwksAlgToRS256 extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client_jwks" })
	public Environment evaluate(Environment env) {

		JsonObject jwks = env.getObject("client_jwks");
		if (jwks == null) {
			throw error("Couldn't find jwks");
		}

		JsonArray keys = jwks.get("keys").getAsJsonArray();

		for (JsonElement e : keys) {
			JsonObject key = e.getAsJsonObject();

			key.addProperty("alg", "RS256");
		}

		logSuccess("Added RS256 as algorithm", args("client_jwks", jwks));

		return env;
	}
}
