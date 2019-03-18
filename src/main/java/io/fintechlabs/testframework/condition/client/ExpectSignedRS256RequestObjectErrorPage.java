package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExpectSignedRS256RequestObjectErrorPage  extends AbstractCondition {

	public ExpectSignedRS256RequestObjectErrorPage(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PostEnvironment(strings = "request_object_unverifiable_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("If the server does not return an invalid_request_object error back to the client, it must show an error page saying the request object is invalid as the algorithm in the JWS header of the request object passed by 'request' parameter does not match the registered.");
		env.putString("request_object_unverifiable_error", placeholder);

		return env;
	}
}
