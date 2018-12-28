package io.fintechlabs.testframework.condition.common;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author srmoore
 *
 */
public class ExpectGrantTypeErrorPage extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public ExpectGrantTypeErrorPage(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PostEnvironment(strings = "grant_type_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder("If the server does not return an error back to the client, " +
				"upload a screenshot of the error page from the use of an invalid grant type.");
		env.putString("grant_type_error", placeholder);

		return env;
	}

}
