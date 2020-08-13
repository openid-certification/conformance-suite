package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectInvalidRequestUriErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "request_uri_invalid_error")
	public Environment evaluate(Environment env) {
		String placeholder = createBrowserInteractionPlaceholder("If the server does not return an error back to the client, It must show an error page that the request_uri is invalid - upload a screenshot of the error page.");

		env.putString("request_uri_invalid_error", placeholder);

		return env;
	}

}
