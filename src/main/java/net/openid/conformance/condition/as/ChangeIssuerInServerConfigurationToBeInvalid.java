package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ChangeIssuerInServerConfigurationToBeInvalid extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		String currentIssuer = OIDFJSON.getString(server.get("issuer"));
		String newIssuer = currentIssuer + "INVALID";
		server.addProperty("issuer", newIssuer);
		env.putObject("server", server);
		log("Added invalid issuer to server configuration", args("issuer", newIssuer));

		return env;
	}
}
