package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExpectResponseModeQueryErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "response_mode_error")
	public Environment evaluate(Environment env) {
		String placeholder = createBrowserInteractionPlaceholder("If the server does not return an error back to the client, " +
			"It must show an error page that the response_mode=query is not allowed by FAPI - upload a screenshot of the error page.");
		env.putString("response_mode_error", placeholder);
		return env;
	}
}
