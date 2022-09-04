package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddRandomJwksUriToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server", strings = "random_jwks_uri_suffix")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		String currentJwksUri = OIDFJSON.getString(server.get("jwks_uri"));
		String randomSuffix = env.getString("random_jwks_uri_suffix");
		String newJwksUri = currentJwksUri + randomSuffix;
		server.addProperty("jwks_uri", newJwksUri);
		env.putObject("server", server);
		log("Added random jwks_uri to server configuration", args("jwks_uri", newJwksUri));

		return env;
	}
}
