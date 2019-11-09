package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

public class RedirectBackToClientWithAuthorizationCodeAndToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request", strings = {"authorization_code", "access_token"})
	@PostEnvironment(strings = "authorization_endpoint_response_redirect")
	public Environment evaluate(Environment env) {

		String redirectUri = env.getString("authorization_endpoint_request", "params.redirect_uri");
		String code = env.getString("authorization_code");
		String state = env.getString("authorization_endpoint_request", "params.state");
		String accessToken = env.getString("access_token");
		String tokenType = env.getString("token_type");

		UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
			.queryParam("code", code)
			.queryParam("access_token", accessToken)
			.queryParam("token_type", tokenType);

		if(state!=null) {
			builder.queryParam("state", state);
		}

		String params = builder.toUriString();
		if(params.startsWith("?")) {
			params = params.substring(1);
		}

		String redirectTo = redirectUri + "#" + params;

		logSuccess("Redirecting back to client", args("uri", redirectTo));

		env.putString("authorization_endpoint_response_redirect", redirectTo);

		return env;

	}

}
