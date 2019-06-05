package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.time.Instant;

public class AddIatValueIsWeekInPastToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_claims")
	@PostEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		Instant iat = Instant.now().minusSeconds(60 * 60 * 24 * 7);

		requestObjectClaims.addProperty("iat", iat.getEpochSecond());

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added iat value to request object which is a week in the past", args("request_object_claims", requestObjectClaims, "iat_is_week_in_the_past", iat));

		return env;

	}

}
