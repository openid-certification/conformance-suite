package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddInvalidAtHashValueToIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims", strings = "at_hash")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String atHash = env.getString("at_hash");

		//Add number 1 onto end of at_hash string
		String concat = (atHash + 1);

		claims.addProperty("at_hash", concat);

		env.putObject("id_token_claims", claims);

		logSuccess("Added invalid at_hash to ID token claims", args("id_token_claims", claims, "invalid_at_hash", concat));

		return env;

	}

}
