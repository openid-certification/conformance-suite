package net.openid.conformance.condition.common;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckForKeyIdInClientJWKs extends AbstractCheckForKeyIdinJWKs {

	@Override
	@PreEnvironment(required = "client_jwks")
	public Environment evaluate(Environment env) {

		return checkForKeyIdInJWKs(env, "client_jwks");
	}

}
