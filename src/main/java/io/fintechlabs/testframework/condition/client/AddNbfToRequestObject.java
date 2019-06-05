package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.time.Instant;

public class AddNbfToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	@PostEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		Instant nbf = Instant.now();

		requestObjectClaims.addProperty("nbf", nbf.getEpochSecond());

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added nbf to request object claims", args(
			"nbf", requestObjectClaims.getAsJsonPrimitive("nbf"))
		);

		return env;
	}
}
