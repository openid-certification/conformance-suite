package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectRequestObjectWithExpOver60ClaimErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "request_object_unverifiable_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("If the server does not return an invalid_request_object error back to the client, it must show an error page saying the request object is invalid as it is using exp value that is more than 60 minutes after the nbf value in signed request object - upload a screenshot of the error page.");
		env.putString("request_object_unverifiable_error", placeholder);

		return env;
	}
}
