package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.AbstractEnsureJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.testmodule.Environment;

public class EnsureEntityStatementJwksDoesNotContainPrivateOrSymmetricKeys extends AbstractEnsureJwksDoesNotContainPrivateOrSymmetricKeys {

	@Override
	@PreEnvironment(required = { "ec_jwks" })
	public Environment evaluate(Environment env) {
		JsonObject jwks = env.getObject("ec_jwks");
		return verifyJwksDoesNotContainPrivateOrSymmetricKeys(env, jwks);
	}
}
