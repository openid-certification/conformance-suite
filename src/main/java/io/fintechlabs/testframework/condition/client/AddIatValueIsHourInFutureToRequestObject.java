package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.time.Instant;

public class AddIatValueIsHourInFutureToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_claims")
	@PostEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		Instant iat = Instant.now().plusSeconds(60 * 60);

		requestObjectClaims.addProperty("iat", iat.getEpochSecond());

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added invalid iat value to request object which is 1 hour in the future", args("request_object_claims", requestObjectClaims, "iat_is_hour_in_the_future", iat));

		return env;

	}

}
