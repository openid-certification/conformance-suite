package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author srmoore
 *
 */
public class GetDynamicClientConfiguration extends AbstractCondition {

	/**
	 * 	 * @param testId
	 * @param log
	 */
	public GetDynamicClientConfiguration(String testId, TestInstanceEventLog log,  ConditionResult conditionResultOnFailure, String... requirements){
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment in) {

		if (!in.containsObj("config")) {
			throw error("Couldn't find a configuration");
		}

		JsonElement client = in.findElement("config", "client");
		if (client == null || !client.isJsonObject()) {
			throw error("Definition for client not present in supplied configuration");
		} else {
			// we've got a client object, put it in the environment
			in.put("client", client.getAsJsonObject());

			// pull out the client name and put it in the root environment for easy access (if there is one)
			String clientName = in.getString("client", "client_name");
			if (!Strings.isNullOrEmpty(clientName)) {
				in.putString("client_name", in.getString("client", "client_name"));
			}
			logSuccess("Found a client object", client.getAsJsonObject());
			return in;
		}
	}
}
