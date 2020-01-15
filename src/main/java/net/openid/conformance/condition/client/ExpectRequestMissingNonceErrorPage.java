package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectRequestMissingNonceErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "invalid_request_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("If the server does not return an invalid_request error back to the client, it must show an error page saying the request is invalid as it is missing the 'nonce' claim - upload a screenshot of the error page.");
		env.putString("invalid_request_error", placeholder);

		return env;
	}

}
