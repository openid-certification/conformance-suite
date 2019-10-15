package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureCodeResponseTypeInClient extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		JsonObject client = env.getObject("client");

		if (!client.has("response_types") || client.getAsJsonArray("response_types").size() != 1) {
			throw error("Missing or invalid number of response_types found in client");
		}
		String responseType = OIDFJSON.getString(client.getAsJsonArray("response_types").get(0));

		if(responseType.equalsIgnoreCase("code")) {
			logSuccess("Found \"code\" response type");
		} else {
			throw error("Invalid response type found: " + responseType);
		}

		return env;
	}
}
