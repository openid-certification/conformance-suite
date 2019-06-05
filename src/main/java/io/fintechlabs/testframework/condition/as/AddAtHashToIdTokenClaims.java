package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddAtHashToIdTokenClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims", strings = "at_hash")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String hash = env.getString("at_hash");

		claims.addProperty("at_hash", hash);

		env.putObject("id_token_claims", claims);

		logSuccess("Added at_hash to ID token claims", args("id_token_claims", claims, "at_hash", hash));

		return env;

	}

}
