package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;


import java.security.SecureRandom;
import java.util.Base64;

public class CreateRandomCodeVerifier extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CreateRandomCodeVerifier(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PostEnvironment(strings = "code_verifier")
	public Environment evaluate(Environment env) {
		SecureRandom sr = new SecureRandom();
		byte[] code = new byte[32];

		sr.nextBytes(code);
		String verifier = Base64.getEncoder().encodeToString(code);

		env.putString("code_verifier", verifier);

		log("Created code_verifier value", args("code_verifier", code));

		return env;
	}
}
