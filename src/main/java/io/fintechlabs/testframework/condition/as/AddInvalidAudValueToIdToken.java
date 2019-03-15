package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddInvalidAudValueToIdToken extends AbstractCondition {

	public AddInvalidAudValueToIdToken(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String aud = env.getElementFromObject("id_token_claims", "aud").getAsString();

		//Add number 1 onto end of aud string
		String concat = (aud + 1);

		claims.addProperty("aud", concat);

		env.putObject("id_token_claims", claims);

		logSuccess("Added invalid aud to ID token claims", args("id_token_claims", claims, "aud", concat));

		return env;

	}

}
