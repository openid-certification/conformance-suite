package net.openid.conformance.condition.common;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractCheckForKeyIdinJWKs extends AbstractCondition {

	protected Environment checkForKeyIdInJWKs(Environment env, String envJWKsKey) {
		JsonElement keys = env.getElementFromObject(envJWKsKey, "keys");
		if (keys == null) {
			throw error("keys entry not found in JWKs");
		}
		if (!keys.isJsonArray()) {
			throw error("keys entry in JWKs is not an array", args("keys", keys));
		}

		for (JsonElement key : keys.getAsJsonArray()) {
			if (!key.isJsonObject()) {
				throw error("invalid key in JWKs, not a JSON object", args("key", key));
			}

			if (!key.getAsJsonObject().has("kid")) {
				throw error("kid not found in key", args("key", key));
			}
		}

		logSuccess("All keys contain kids");

		return env;
	}
}
