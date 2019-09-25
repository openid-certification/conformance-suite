package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractAccessTokenFromAuthorizationResponse extends AbstractExtractAccessToken {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	@PostEnvironment(required = "access_token")
	public Environment evaluate(Environment env) {

		return extractAccessToken(env, "authorization_endpoint_response");
	}

}
