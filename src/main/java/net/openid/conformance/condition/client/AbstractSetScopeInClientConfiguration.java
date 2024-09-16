package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractSetScopeInClientConfiguration extends AbstractCondition {

	protected Environment setScopeInClientConfiguration(Environment env, String scope) {
		return setScopeInClientConfiguration(env, scope, "");
	}

	protected Environment setScopeInClientConfiguration(Environment env, String scope, String additionalLogMsg) {
		JsonObject client = env.getObject("client");
		client.addProperty("scope", scope);
		env.putObject("client", client);
		log(("Set scope in client configuration to \"%s\"" + additionalLogMsg).formatted(scope), args("scope", scope));
		return env;
	}
}
