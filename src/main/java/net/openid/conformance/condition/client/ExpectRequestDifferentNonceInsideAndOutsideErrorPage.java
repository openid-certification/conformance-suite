package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectRequestDifferentNonceInsideAndOutsideErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "request_unverifiable_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("If the server does not return an invalid_request error back to the client, it must either show an error page (saying the request is invalid as the 'nonce' value in the request object and outside it are different - upload a screenshot of the error page) or must successfully authenticate and but return the nonce from inside the request object.");
		env.putString("request_unverifiable_error", placeholder);

		return env;
	}

}
