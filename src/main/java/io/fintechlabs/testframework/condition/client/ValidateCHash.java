package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateCHash extends ValidateHash {

	@Override
	@PreEnvironment(required = { "c_hash" , "callback_params" })
	public Environment evaluate(Environment env) {
		return super.validateHash(env,"c_hash","c_hash");
	}


}
