package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateServerJWKs extends AbstractValidateJWKs {

	@Override
	@PreEnvironment(required = "server_jwks")
	public Environment evaluate(Environment env) {
		JsonObject jwks = env.getObject("server_jwks");

		checkJWKs(jwks);

		logSuccess("Valid server JWKs");

		return env;
	}
}
