package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class OBValidateIdTokenIntentId extends AbstractCondition {

	public OBValidateIdTokenIntentId(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}


	@Override
	@PreEnvironment(required = { "id_token" }, strings = "account_request_id")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("id_token")) {
			throw error("Couldn't find parsed ID token");
		}

		String obIntentId = env.getString("id_token", "claims.openbanking_intent_id");

		if (obIntentId == null) {
			throw error("id_token does not contain the required openbanking_intent_id claim");
		}

		if (!env.getString("account_request_id").equals(obIntentId)) {
			throw error("openbanking_intent_id in id_token does not match the expected value",
				args("id_token", obIntentId, "expected", env.getString("account_request_id")));
		}

		logSuccess("openbanking_intent_id passed all validation checks");
		return env;
	}

}
