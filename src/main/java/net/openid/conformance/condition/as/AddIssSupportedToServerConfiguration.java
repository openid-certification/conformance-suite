package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddIssSupportedToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");
		server.addProperty("authorization_response_iss_parameter_supported", true);

		logSuccess("Added 'authorization_response_iss_parameter_supported' as 'true' to server metadata");

		return env;
	}
}
