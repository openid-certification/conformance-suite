package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Adds request_uri_method=post to the redirect URL as a query parameter.
 *
 * Per OID4VP section 5.1, request_uri_method must be available as a URL query parameter
 * (not only inside the request object) because the wallet needs it to determine whether
 * to GET or POST when fetching the request_uri.
 */
public class AddRequestUriMethodPostToRedirectUrl extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "redirect_to_authorization_endpoint")
	@PostEnvironment(strings = "redirect_to_authorization_endpoint")
	public Environment evaluate(Environment env) {

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		String updatedRedirectTo = UriComponentsBuilder.fromUriString(redirectTo)
			.queryParam("request_uri_method", "post")
			.toUriString();

		env.putString("redirect_to_authorization_endpoint", updatedRedirectTo);

		logSuccess("Added request_uri_method=post to redirect URL",
			args("redirect_to_authorization_endpoint", updatedRedirectTo));

		return env;
	}

}
