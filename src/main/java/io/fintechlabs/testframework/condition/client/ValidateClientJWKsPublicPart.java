package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateClientJWKsPublicPart extends AbstractValidateJWKs {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JsonElement jwks = env.getElementFromObject("client", "jwks");

		checkJWKs(jwks, false);

		logSuccess("Valid client JWKs");

		return env;
	}
}
