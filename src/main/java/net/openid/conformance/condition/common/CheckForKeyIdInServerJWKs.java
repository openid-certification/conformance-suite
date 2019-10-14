package net.openid.conformance.condition.common;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckForKeyIdInServerJWKs extends AbstractCheckForKeyIdinJWKs {

	@Override
	@PreEnvironment(required = "server_jwks")
	public Environment evaluate(Environment env) {
		return checkForKeyIdInJWKs(env, "server_jwks");
	}

}
