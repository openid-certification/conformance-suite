package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.time.Instant;

public class AddInvalidExpiredExpValueToIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		Instant exp = Instant.now().minusSeconds(60 * 6);

		claims.addProperty("exp", exp.getEpochSecond());

		env.putObject("id_token_claims", claims);

		logSuccess("Added expired exp value to ID token claims", args("id_token_claims", claims, "exp", exp));

		return env;

	}

}
