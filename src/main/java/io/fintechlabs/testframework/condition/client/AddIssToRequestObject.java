package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddIssToRequestObject extends AbstractCondition {

	public AddIssToRequestObject(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		String clientId = env.getString("client", "client_id");

		if (clientId != null) {

			requestObjectClaims.addProperty("iss", clientId);

			env.putObject("request_object_claims", requestObjectClaims);

			logSuccess("Added iss to request object claims", args("iss", clientId));

		} else {

			// Only a "should" requirement
			log("Request object contains no issuer and client ID not found");

		}

		return env;
	}
}
