package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExpectAccessDeniedErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "request_object_unverifiable_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("If the server does not return an access_denied error back to the client, it must show an error page saying that acr 'urn:openbanking:psd2:sca' is not permitted.");
		env.putString("request_object_unverifiable_error", placeholder);

		return env;
	}
}
