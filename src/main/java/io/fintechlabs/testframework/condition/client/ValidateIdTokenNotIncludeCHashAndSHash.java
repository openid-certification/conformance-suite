package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateIdTokenNotIncludeCHashAndSHash extends AbstractCondition {

	public ValidateIdTokenNotIncludeCHashAndSHash(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment( required = "id_token" )
	public Environment evaluate(Environment env) {

		JsonElement claimElement = env.getElementFromObject("id_token", "claims");

		if (claimElement == null || !claimElement.isJsonObject()) {
			log("Skipped to check claims that is null or not a json");
		}

		JsonObject claims = claimElement.getAsJsonObject();

		if (claims.has("s_hash") || claims.has("c_hash")) {
			throw error("claims contains 'c_hash' or 's_hash'", args("claims", claims));
		}

		logSuccess("id_token claims correctly does not contain 'c_hash' and 's_hash'", args("claims", claims));

		return env;
	}
}
