package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExpectRequestObjectBadIntentIdErrorPage extends AbstractCondition {

	public ExpectRequestObjectBadIntentIdErrorPage(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PostEnvironment(strings = "request_object_bad_intentid_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("Show error page showing an error saying the request object contains an invalid intent id");
		env.putString("request_object_bad_intentid_error", placeholder);

		return env;
	}

}
