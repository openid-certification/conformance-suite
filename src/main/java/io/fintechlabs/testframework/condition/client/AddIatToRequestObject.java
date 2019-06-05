package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.time.Instant;

public class AddIatToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		Instant iat = Instant.now();

		requestObjectClaims.addProperty("iat", iat.getEpochSecond());

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added iat to request object claims", args(
			"iat", requestObjectClaims.getAsJsonPrimitive("iat"))
		);

		return env;
	}
}
