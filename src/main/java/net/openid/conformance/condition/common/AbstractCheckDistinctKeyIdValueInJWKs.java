package net.openid.conformance.condition.common;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractCheckDistinctKeyIdValueInJWKs extends AbstractCondition {

	protected Environment checkDistinctKeyIdValueInJWKs(Environment env, String envJWKsKey) {

		JsonElement keys = env.getElementFromObject(envJWKsKey, "keys");

		if (keys == null) {
			throw error("keys entry not found in JWKs");
		}

		if (!keys.isJsonArray()) {
			throw error("keys entry in JWKs is not an array", args("keys", keys));
		}

		Set<String> keyIdSets = new HashSet<>();
		for (JsonElement key : keys.getAsJsonArray()) {
			if (!key.isJsonObject()) {
				throw error("invalid key in JWKs, not a JSON object", args("key", key));
			}

			String keyId = OIDFJSON.getString(key.getAsJsonObject().get("kid"));
			if (!keyIdSets.add(keyId)) {
				throw error("'kid' value is used more than once in server JWKs", args("kid_duplicate", keyId, "keys", keys));
			}
		}

		logSuccess("Distinct 'kid' value in all keys of server JWKs");
		return env;
	}
}
