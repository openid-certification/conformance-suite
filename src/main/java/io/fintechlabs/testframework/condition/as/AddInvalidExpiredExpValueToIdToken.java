package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddInvalidExpiredExpValueToIdToken extends AbstractCondition {

	public AddInvalidExpiredExpValueToIdToken(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		Long exp = env.getLong("id_token_claims", "exp");

		Long expSubtract6Mins = (exp - 360);

		claims.addProperty("exp", expSubtract6Mins);

		env.putObject("id_token_claims", claims);

		logSuccess("Added expired exp value to ID token claims", args("id_token_claims", claims, "exp", expSubtract6Mins));

		return env;

	}

}
