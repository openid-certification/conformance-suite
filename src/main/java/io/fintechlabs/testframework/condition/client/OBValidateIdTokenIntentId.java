package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class OBValidateIdTokenIntentId extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public OBValidateIdTokenIntentId(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}
	
	
	@Override
	@PreEnvironment(required = { "id_token" }, strings = "account_request_id")
	public Environment evaluate(Environment env) {

		if (!env.containsObj("id_token")) {
			return error("Couldn't find parsed ID token");
		}
		
		String obIntentId = env.getString("id_token", "claims.openbanking_intent_id");
		
		if (obIntentId == null) {
			return error("Missing openbanking_intent_id");
		}
		
		if (!env.getString("account_request_id").equals(obIntentId)) {
			return error("Mismatch: openbanking_intent_id(server):" + obIntentId + ". Expected:" + env.getString("account_request_id"));
		} 
		
		logSuccess("openbanking_intent_id passed all validation checks");
		return env;
	}

}
