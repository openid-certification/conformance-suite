package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddBadAudToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {
		JsonObject requestObjectClaims = env.getObject("request_object_claims");
		requestObjectClaims.addProperty("aud", "https://www.other1.example.com/");
		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added bad aud to request object claims", args(
			"aud", requestObjectClaims.getAsJsonPrimitive("aud")));
		return env;
	}
}
