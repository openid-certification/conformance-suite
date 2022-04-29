package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddScopesSupportedOpenIdToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonArray scopes = new JsonArray();
		scopes.add("openid");

		JsonObject server = env.getObject("server");
		server.add("scopes_supported", scopes);

		logSuccess("Added 'scopes_supported' as 'openid' to server metadata");

		return env;
	}
}
