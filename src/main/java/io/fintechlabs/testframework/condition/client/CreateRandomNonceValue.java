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
public class CreateRandomNonceValue extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public CreateRandomNonceValue(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PostEnvironment(strings = "nonce")
	public Environment evaluate(Environment env) {

		String nonce = RandomStringUtils.randomAlphanumeric(10);
		env.putString("nonce", nonce);

		log("Created nonce value", args("nonce", nonce));

		return env;

	}

}
