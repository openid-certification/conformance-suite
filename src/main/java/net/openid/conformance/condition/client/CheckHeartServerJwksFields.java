package net.openid.conformance.condition.client;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckHeartServerJwksFields extends AbstractCondition {

	@PreEnvironment(required = "server_jwks")
	@Override
	public Environment evaluate(Environment env) {

		JsonObject jwks = env.getObject("server_jwks");

		JsonArray keys = jwks.get("keys").getAsJsonArray();

		List<String> required = ImmutableList.of("kid", "alg", "kty");

		for (JsonElement e : keys) {
			JsonObject key = e.getAsJsonObject();

			for (String req : required) {
				if (!key.has(req)) {
					throw error("Key missing required field", args("missing", req, "key", key));
				}
			}
		}

		logSuccess("All keys contain required field", args("required", required));

		return env;
	}

}
