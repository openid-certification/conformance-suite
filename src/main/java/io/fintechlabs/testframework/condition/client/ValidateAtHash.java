package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateAtHash extends ValidateHash {

	@Override
	@PreEnvironment(required = { "access_token", "at_hash" } )
	public Environment evaluate(Environment env) {
		return super.validateHash(env,"at_hash","at_hash");
	}


}
