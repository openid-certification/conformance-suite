package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class FAPIValidateIdTokenSigningAlg extends AbstractCondition {

	public FAPIValidateIdTokenSigningAlg(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(strings = { "id_token_signing_alg" })
	public Environment evaluate(Environment env) {

		String alg = env.getString("id_token_signing_alg");

		if (alg.equals("PS256") || alg.equals("ES256")) {
			logSuccess("id_token was signed with a permitted algorithm", args("alg", alg));
			return env;
		}

		throw error("id_token should be signed with PS256 or ES256", args("alg", alg));
	}

}
