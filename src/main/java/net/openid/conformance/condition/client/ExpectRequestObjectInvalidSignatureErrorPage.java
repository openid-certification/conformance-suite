package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectRequestObjectInvalidSignatureErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "request_object_unverifiable_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("If the server does not return an invalid_request_object error back to the client, it must show an error page saying that the signature of the request object passed by 'request' parameter was not verified.");
		env.putString("request_object_unverifiable_error", placeholder);

		return env;
	}
}
