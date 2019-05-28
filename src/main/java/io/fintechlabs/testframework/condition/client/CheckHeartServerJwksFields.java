package io.fintechlabs.testframework.condition.client;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckHeartServerJwksFields extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public CheckHeartServerJwksFields(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
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
