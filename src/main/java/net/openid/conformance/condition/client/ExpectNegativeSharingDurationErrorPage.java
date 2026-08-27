package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectNegativeSharingDurationErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "request_object_unverifiable_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("If the server does not return an error back to the client, it should show an error page saying the requested sharing_duration is invalid - upload a screenshot of the error page.");
		env.putString("request_object_unverifiable_error", placeholder);

		return env;
	}
}
