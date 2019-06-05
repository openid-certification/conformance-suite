package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateSHash extends ValidateHash {

	@Override
	@PreEnvironment(strings = "state", required = "s_hash")
	public Environment evaluate(Environment env) {
		return super.validateHash(env, "s_hash", "s_hash");
	}

}
