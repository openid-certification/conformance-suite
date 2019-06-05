package io.fintechlabs.testframework.condition.common;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckForKeyIdInServerJWKs extends AbstractCheckForKeyIdinJWKs {

	@Override
	@PreEnvironment(required = "server_jwks")
	public Environment evaluate(Environment env) {
		return checkForKeyIdInJWKs(env, "server_jwks");
	}

}
