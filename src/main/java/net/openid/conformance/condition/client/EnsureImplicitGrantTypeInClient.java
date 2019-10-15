package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

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
