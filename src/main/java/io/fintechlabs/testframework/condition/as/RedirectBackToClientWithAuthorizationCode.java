package io.fintechlabs.testframework.condition.as;

import org.springframework.web.util.UriComponentsBuilder;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class RedirectBackToClientWithAuthorizationCode extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request", strings = "authorization_code")
	@PostEnvironment(strings = "authorization_endpoint_response_redirect")
	public Environment evaluate(Environment env) {

		String redirectUri = env.getString("authorization_endpoint_request", "params.redirect_uri");
		String code = env.getString("authorization_code");
		String state = env.getString("authorization_endpoint_request", "params.state");

		String redirectTo = UriComponentsBuilder.fromHttpUrl(redirectUri)
			.queryParam("state", state)
			.queryParam("code", code)
			.toUriString();

		logSuccess("Redirecting back to client", args("uri", redirectTo));

		env.putString("authorization_endpoint_response_redirect", redirectTo);

		return env;

	}

}
