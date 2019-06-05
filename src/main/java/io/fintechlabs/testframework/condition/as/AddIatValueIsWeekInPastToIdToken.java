package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.time.Instant;

public class AddIatValueIsWeekInPastToIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		Instant iat = Instant.now().minusSeconds(60 * 60 * 24 * 7);

		claims.addProperty("iat", iat.getEpochSecond());

		env.putObject("id_token_claims", claims);

		logSuccess("Added iat value which is a week in the past to ID token claims", args("id_token_claims", claims, "iat_seven_days_in_the_past", iat));

		return env;

	}

}
