package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractAccessTokenFromAuthorizationResponse extends AbstractExtractAccessToken {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	@PostEnvironment(required = "access_token")
	public Environment evaluate(Environment env) {

		return extractAccessToken(env, "authorization_endpoint_response");
	}

}
