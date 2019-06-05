package io.fintechlabs.testframework.condition.common;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckForKeyIdInClientJWKs extends AbstractCheckForKeyIdinJWKs {

	@Override
	@PreEnvironment(required = "client_jwks")
	public Environment evaluate(Environment env) {

		return checkForKeyIdInJWKs(env, "client_jwks");
	}

}
