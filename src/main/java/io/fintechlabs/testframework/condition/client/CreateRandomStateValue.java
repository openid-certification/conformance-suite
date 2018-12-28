package io.fintechlabs.testframework.condition.client;

import org.apache.commons.lang3.RandomStringUtils;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CreateRandomStateValue extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CreateRandomStateValue(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PostEnvironment(strings = "state")
	public Environment evaluate(Environment env) {

		Integer stateLength = env.getInteger("requested_state_length");

		if (stateLength == null) {
			stateLength = 10; // default to a state of length 10
		}

		String state = RandomStringUtils.randomAlphanumeric(stateLength);
		env.putString("state", state);

		log("Created state value", args("state", state, "requested_state_length", stateLength));

		return env;
	}

}
