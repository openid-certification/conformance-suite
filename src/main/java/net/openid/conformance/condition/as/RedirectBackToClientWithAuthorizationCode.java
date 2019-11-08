package net.openid.conformance.condition.as;

import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;

public class RedirectBackToClientWithAuthorizationCode extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request", strings = "authorization_code")
	@PostEnvironment(strings = "authorization_endpoint_response_redirect")
	public Environment evaluate(Environment env) {

		String redirectUri = env.getString("authorization_endpoint_request", "params.redirect_uri");
		String code = env.getString("authorization_code");
		String state = env.getString("authorization_endpoint_request", "params.state");

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(redirectUri);
		if(state!=null) {
			builder.queryParam("state", state);
		}
		builder.queryParam("code", code);

		String redirectTo = builder.toUriString();

		logSuccess("Redirecting back to client", args("uri", redirectTo));

		env.putString("authorization_endpoint_response_redirect", redirectTo);

		return env;

	}

}
