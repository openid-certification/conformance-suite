package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractAccessTokenFromTokenResponse extends AbstractExtractAccessToken {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	@PostEnvironment(required = "access_token")
	public Environment evaluate(Environment env) {

		return extractAccessToken(env, "token_endpoint_response");
	}

}
