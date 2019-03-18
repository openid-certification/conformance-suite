package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class TestCanOnlyBePerformedForPS256Alg extends AbstractCondition {

	public TestCanOnlyBePerformedForPS256Alg(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	public Environment evaluate(Environment env) {

		JsonObject jwks = env.getObject("client_jwks");
		if (jwks == null) {
			throw error("Couldn't find jwks");
		}

		JsonArray keys = jwks.get("keys").getAsJsonArray();
		if (keys == null || keys.size() <= 0) {
			throw error("Keys in client_jwks can not be null or empty.");
		}

		// Get first key that use to sign request_object or client_assertion
		JsonObject key = keys.get(0).getAsJsonObject();
		String alg = key.get("alg").getAsString();
		logSuccess(String.format("This test requires RSA keys to be performed, the alg in client configuration is '%s' so this test is being skipped.", alg));

		return env;
	}
}
