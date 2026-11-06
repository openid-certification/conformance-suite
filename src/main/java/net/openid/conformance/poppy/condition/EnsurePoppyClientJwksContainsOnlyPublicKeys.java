package net.openid.conformance.poppy.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.AbstractEnsureJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.testmodule.Environment;

public class EnsurePoppyClientJwksContainsOnlyPublicKeys extends AbstractEnsureJwksDoesNotContainPrivateOrSymmetricKeys {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		return verifyJwksDoesNotContainPrivateOrSymmetricKeys(env, env.getElementFromObject("client", "jwks").getAsJsonObject());
	}
}
