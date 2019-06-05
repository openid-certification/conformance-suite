package io.fintechlabs.testframework.condition.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public class FAPICheckKeyAlgInClientJWKs extends AbstractCondition {

	public FAPICheckKeyAlgInClientJWKs(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "client_jwks")
	public Environment evaluate(Environment env) {
		JsonElement keys = env.getElementFromObject("client_jwks", "keys");
		if (keys == null || !keys.isJsonArray()) {
			throw error("keys array not found in JWKs");
		}

		boolean found = false;
		for (JsonElement key : keys.getAsJsonArray()) {
			if (!key.isJsonObject()) {
				throw error("invalid key in JWKs", args("key", key));
			}
			JsonObject keyObj = key.getAsJsonObject();

			if (!keyObj.has("alg")) {
				throw error("alg not found in key", args("key", key));
			}

			String alg = OIDFJSON.getString(keyObj.getAsJsonPrimitive("alg"));
			if (alg.equals("PS256") || alg.equals("ES256")) {
				found = true;
			}
		}

		if (!found) {
			throw error("client jwks key should have alg PS256 or ES256", args("keys", keys));
		}

		logSuccess("Found a key with alg PS256 or ES256");

		return env;
	}

}
