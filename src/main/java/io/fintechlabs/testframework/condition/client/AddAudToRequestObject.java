package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddAudToRequestObject extends AbstractCondition {

	public AddAudToRequestObject(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		String serverIssuerUrl = env.getString("server", "issuer");

		if (serverIssuerUrl != null) {

			requestObjectClaims.addProperty("aud", serverIssuerUrl);

			env.putObject("request_object_claims", requestObjectClaims);

			logSuccess("Added aud to request object claims", args("aud", serverIssuerUrl));

		} else {

			// Only a "should" requirement
			log("Request object contains no audience and server issuer URL not found");

		}

		return env;
	}
}
