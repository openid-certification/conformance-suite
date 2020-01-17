package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys extends AbstractEnsureJwksDoesNotContainPrivateOrSymmetricKeys {

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {

		JsonObject client = env.getObject("client");

		JsonObject jwks = client.getAsJsonObject("jwks");

		if(jwks==null) {
			throw error("Client does not contain a jwks element", args("client", client));
		}

		return verifyJwksDoesNotContainPrivateOrSymmetricKeys(env, jwks);
	}

}
