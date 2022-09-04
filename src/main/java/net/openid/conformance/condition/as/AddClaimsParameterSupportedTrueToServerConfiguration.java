package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddClaimsParameterSupportedTrueToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {
		JsonObject server = env.getObject("server");
		server.addProperty("claims_parameter_supported", true);

		log("Successfully added claims_parameter_supported to server configuration", args("server", server));
		return env;
	}

}
