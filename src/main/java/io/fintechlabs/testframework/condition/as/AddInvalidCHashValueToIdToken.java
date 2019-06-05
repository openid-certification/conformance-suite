package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddInvalidCHashValueToIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims", strings = "c_hash")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String hash = env.getString("c_hash");

		//Add number 1 onto end of hash string
		String concat = (hash + 1);

		claims.addProperty("c_hash", concat);

		env.putObject("id_token_claims", claims);

		logSuccess("Added invalid c_hash to ID token claims", args("id_token_claims", claims, "c_hash", concat));

		return env;

	}

}
