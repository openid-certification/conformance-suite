package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractRefreshTokenFromTokenResponse extends AbstractCondition {
	@Override
	@PreEnvironment(required = {"token_endpoint_response"})
	@PostEnvironment(strings = {"refresh_token"})
	public Environment evaluate(Environment env) {
		String refreshToken = env.getString("token_endpoint_response", "refresh_token");
		if(refreshToken==null) {
			throw error("Token endpoint response does not contain a refresh token");
		}
		env.putString("refresh_token", refreshToken);
		logSuccess("Extracted refresh token from response", args("refresh_token", refreshToken));
		return env;
	}
}
