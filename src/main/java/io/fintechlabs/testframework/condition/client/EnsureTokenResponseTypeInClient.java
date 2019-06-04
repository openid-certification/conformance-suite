package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public class EnsureTokenResponseTypeInClient extends AbstractCondition {

	public EnsureTokenResponseTypeInClient(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		JsonObject client = env.getObject("client");

		if (!client.has("response_types") || client.getAsJsonArray("response_types").size() != 1) {
			throw error("Missing or invalid number of response_types found in client");
		}
		String responseType = OIDFJSON.getString(client.getAsJsonArray("response_types").get(0));

		if(responseType.equalsIgnoreCase("token")) {
			logSuccess("Found \"token\" response type");
		} else {
			throw error("Invalid response type found: " + responseType);
		}

		return env;
	}
}
