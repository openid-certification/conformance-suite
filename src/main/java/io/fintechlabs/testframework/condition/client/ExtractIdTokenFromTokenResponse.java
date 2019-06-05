package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractExtractIdToken;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractIdTokenFromTokenResponse extends AbstractExtractIdToken {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	@PostEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {
		return extractIdToken(env, "token_endpoint_response");
	}

}
