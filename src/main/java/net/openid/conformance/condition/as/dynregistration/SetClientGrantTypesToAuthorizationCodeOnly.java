package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetClientGrantTypesToAuthorizationCodeOnly extends AbstractCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");
		JsonArray grantTypes = new JsonArray();
		grantTypes.add("authorization_code");
		client.add("grant_types", grantTypes);
		env.putObject("client", client);
		log("Set grant_types to ['authorization_code'] for the registered client", args("client", client));
		return env;
	}
}
