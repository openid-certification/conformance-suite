package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddInvalidOpenBankingIntentIdToIdToken extends AbstractCondition {

	public AddInvalidOpenBankingIntentIdToIdToken(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "id_token_claims", strings = "openbanking_intent_id")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String intent = env.getString("openbanking_intent_id");

		//Add number 1 onto end of intent string
		String concat = intent + 1;

		claims.addProperty("openbanking_intent_id", concat);

		env.putObject("id_token_claims", claims);

		logSuccess("Added invalid openbanking_intent_id to ID token claims", args("id_token_claims", claims, "openbanking_intent_id", concat));

		return env;

	}

}
