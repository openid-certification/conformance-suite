package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractAddScopeToClientConfiguration extends AbstractCondition {

	protected Environment addScopeToClientConfiguration(Environment env, String scope) {
		JsonObject client = env.getObject("client");
		client.addProperty("scope", "openid " + scope);
		env.putObject("client", client);
		log(String.format("Added \"%s\" to client configuration", scope), args("scope", scope));
		return env;
	}
}
