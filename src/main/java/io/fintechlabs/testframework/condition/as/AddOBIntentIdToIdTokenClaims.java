package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddOBIntentIdToIdTokenClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims", strings = "openbanking_intent_id")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");
		String intentId = env.getString("openbanking_intent_id");

		claims.addProperty("openbanking_intent_id", intentId);

		env.putObject("id_token_claims", claims);

		logSuccess("Added intent ID to ID token claims", args("id_token_claims", claims, "openbanking_intent_id", intentId));

		return env;

	}

}
