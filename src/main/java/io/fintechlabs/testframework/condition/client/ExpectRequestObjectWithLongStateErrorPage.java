package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExpectRequestObjectWithLongStateErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "request_object_unverifiable_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("If the server does not return an invalid_request error back to the client, it must show an error page (saying server rejects long state at authorization endpoint - upload a screenshot of the error page) or must successfully authenticate and return the state correctly.");
		env.putString("request_object_unverifiable_error", placeholder);

		return env;
	}

}
