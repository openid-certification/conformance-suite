package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureClientDoesNotHaveBothJwksAndJwksUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {

		JsonObject client = env.getObject("client");

		if (client.has("jwks") && client.has("jwks_uri")) {

			throw error("Client cannot have both jwks and jwks_uri at the same time", args("client", client));

		}
		logSuccess("Client does not have both jwks and jwks_uri set", args("client", client));
		return env;
	}

}
