package net.openid.conformance.openbanking_brasil;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * This condition actually performs an assertion.
 * In this contrived example, we are simply expecting a hard-coded
 * message to be present. If not, we throw the ConditionError which the
 * test runner captures and fails the test.
 */
public class SimpleMessageAssert extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "message")
	public Environment evaluate(Environment env) {
		String message = env.getString("message");
		if(!message.equals("Hello")) {
			logFailure("Message not correct");
			throw new ConditionError(getTestId(), "Message was not 'Hello'");
		}
		logSuccess("Message was correct!");
		return env;
	}

}
