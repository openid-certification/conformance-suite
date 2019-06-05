package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddAlgorithmAsRS256 extends AbstractCondition {

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
