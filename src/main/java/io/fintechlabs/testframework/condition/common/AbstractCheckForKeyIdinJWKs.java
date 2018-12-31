package io.fintechlabs.testframework.condition.common;

import com.google.gson.JsonElement;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public abstract class AbstractCheckForKeyIdinJWKs extends AbstractCondition {
	public AbstractCheckForKeyIdinJWKs(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	protected Environment checkForKeyIdInJWKs(Environment env, String envJWKsKey) {
		JsonElement keys = env.getElementFromObject(envJWKsKey, "keys");
		if (keys == null || !keys.isJsonArray()) {
			throw error("keys array not found in JWKs");
		}

		for (JsonElement key : keys.getAsJsonArray()) {
			if (!key.isJsonObject()) {
				throw error("invalid key in JWKs", args("key", key));
			}

			if (!key.getAsJsonObject().has("kid")) {
				throw error("kid not found in key", args("key", key));
			}
		}

		logSuccess("All keys contain kids");

		return env;
	}
}
