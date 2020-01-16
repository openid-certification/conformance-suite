package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys extends AbstractEnsureJwksDoesNotContainPrivateOrSymmetricKeys {

	@Override
	@PreEnvironment(required = { "server_jwks"})
	public Environment evaluate(Environment env) {

		JsonObject jwks = env.getObject("server_jwks");
		return verifyJwksDoesNotContainPrivateOrSymmetricKeys(env, jwks);

	}

}
