package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class GetStaticClientConfiguration extends AbstractCondition {

	public GetStaticClientConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "client", strings = "client_id")
	public Environment evaluate(Environment env) {
		// make sure we've got a client object
		JsonElement client = env.getElementFromObject("config", "client");
		if (client == null || !client.isJsonObject()) {
			throw error("Definition for client not present in supplied configuration");
		} else {
			// we've got a client object, put it in the environment
			env.putObject("client", client.getAsJsonObject());

			// pull out the client ID and put it in the root environment for easy access
			env.putString("client_id", env.getString("client", "client_id"));

			logSuccess("Found a static client object", client.getAsJsonObject());
			return env;
		}
	}

}
