package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public class EnsureImplicitGrantTypeInClient extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		JsonObject client = env.getObject("client");

		if (!client.has("grant_types") || client.getAsJsonArray("grant_types").size() != 1) {
			throw error("Missing or invalid number of grant_types found in client");
		}
		String grantType = OIDFJSON.getString(client.getAsJsonArray("grant_types").get(0));

		if(grantType.equalsIgnoreCase("implicit")) {
			logSuccess("Found \"implicit\" grant type");
		} else {
			throw error("Invalid grant type found: " + grantType);
		}

		return env;
	}
}
