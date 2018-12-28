package io.fintechlabs.testframework.condition.client;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;


public class ExtractExpiresInFromTokenEndpointResponse extends AbstractCondition {

	public ExtractExpiresInFromTokenEndpointResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	@PostEnvironment(required = "expires_in")
	public Environment evaluate(Environment env) {
		JsonObject tokenEndpoint = env.getObject("token_endpoint_response");

		JsonElement expiresInValue = tokenEndpoint.get("expires_in");
		if (expiresInValue == null) {
			log("Couldn't find 'expires_in'", tokenEndpoint);
			return env;
		}

		/* Create our cut down JsonObject with just a single value in it */
		JsonObject value = new JsonObject();
		value.add("expires_in", expiresInValue);
		env.putObject("expires_in", value);

		logSuccess("Extracted 'expires_in'", value);

		return env;

	}
}
