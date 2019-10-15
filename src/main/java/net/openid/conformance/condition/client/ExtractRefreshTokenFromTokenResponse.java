package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractRefreshTokenFromTokenResponse extends AbstractCondition {
	@Override
	@PreEnvironment(required = {"token_endpoint_response"})
	@PostEnvironment(strings = {"refresh_token"})
	public Environment evaluate(Environment env) {
		String refreshToken = env.getString("token_endpoint_response", "refresh_token");
		if(refreshToken==null) {
			// It's perfectly legal to NOT return a new refresh token; if the server didn't then
			// 'refresh_token' in the environment will be left containing the old (still valid)
			// token. We use that token later to test the refresh token is bound to the client
			// correctly.
			throw error("Token endpoint response does not contain a refresh token");
		}
		env.putString("refresh_token", refreshToken);
		logSuccess("Extracted refresh token from response", args("refresh_token", refreshToken));
		return env;
	}
}
