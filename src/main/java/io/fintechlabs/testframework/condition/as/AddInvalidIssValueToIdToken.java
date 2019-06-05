package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddInvalidIssValueToIdToken extends AbstractCondition {

	public AddInvalidIssValueToIdToken(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String iss = env.getString("id_token_claims", "iss");

		//Add number 1 onto end of iss string
		String concat = (iss + 1);

		claims.addProperty("iss", concat);

		env.putObject("id_token_claims", claims);

		logSuccess("Added invalid iss to ID token claims", args("id_token_claims", claims, "iss", concat));

		return env;

	}

}
