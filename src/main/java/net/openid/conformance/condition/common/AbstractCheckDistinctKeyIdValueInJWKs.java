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

			JsonElement keyIdElement = key.getAsJsonObject().get("kid");
			if (keyIdElement != null && !keyIdSets.add(OIDFJSON.getString(keyIdElement))) {
				throw error("'kid' value is used more than once in "+envJWKsKey,
					args("kid_duplicate", OIDFJSON.getString(keyIdElement),
						"keys", keys,
						"see", "https://bitbucket.org/openid/connect/issues/1127"));
			}
		}

		logSuccess("Distinct 'kid' value in all keys of "+envJWKsKey, args("see", "https://bitbucket.org/openid/connect/issues/1127"));
		return env;
	}
}
