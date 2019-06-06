package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateIdTokenNonce extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {

		String incomingNonce = env.getString("id_token","claims.nonce");

		if (!env.getString("nonce").equals(incomingNonce)) {
			throw error("Nonce values mismatch", args("actual", incomingNonce, "expected", env.getString("nonce")));
		} else {
			logSuccess("Nonce values match", args("nonce", incomingNonce));
		}

		return env;
	}

}
