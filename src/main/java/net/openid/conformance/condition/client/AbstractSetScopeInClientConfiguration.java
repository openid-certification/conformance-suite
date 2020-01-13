package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractSetScopeInClientConfiguration extends AbstractCondition {

	protected Environment setScopeInClientConfiguration(Environment env, String scope) {
		JsonObject client = env.getObject("client");
		client.addProperty("scope", scope);
		env.putObject("client", client);
		log(String.format("Set scope in client configuration to \"%s\"", scope), args("scope", scope));
		return env;
	}
}
