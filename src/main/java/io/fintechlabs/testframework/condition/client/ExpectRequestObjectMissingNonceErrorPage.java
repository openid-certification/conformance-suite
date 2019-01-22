package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExpectRequestObjectMissingNonceErrorPage extends AbstractCondition {

	public ExpectRequestObjectMissingNonceErrorPage(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PostEnvironment(strings = "request_object_unverifiable_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("If the server does not return an invalid_request_object error back to the client, it must show an error page saying the request object is invalid as it is missing the 'nonce' claim - upload a screenshot of the error page.");
		env.putString("request_object_unverifiable_error", placeholder);

		return env;
	}

}
