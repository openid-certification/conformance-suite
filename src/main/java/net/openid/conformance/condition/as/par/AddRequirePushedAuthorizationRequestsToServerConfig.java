package net.openid.conformance.condition.as.par;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddRequirePushedAuthorizationRequestsToServerConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		server.addProperty("require_pushed_authorization_requests", true);

		log("Added require_pushed_authorization_requests to server configuration", args("value", true));

		return env;
	}
}
