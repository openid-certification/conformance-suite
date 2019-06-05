package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractCHash extends ExtractHash {

	@Override
	@PreEnvironment(required = "id_token")
	@PostEnvironment(required = "c_hash")
	public Environment evaluate(Environment env) {

		return super.extractHash(env, "c_hash", "c_hash");

	}

}
