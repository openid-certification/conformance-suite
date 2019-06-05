package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractSHash extends ExtractHash {

	@Override
	@PreEnvironment(required = "id_token")
	@PostEnvironment(required = "s_hash")
	public Environment evaluate(Environment env) {

		return super.extractHash(env, "s_hash", "s_hash");

	}

}
