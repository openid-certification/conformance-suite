package io.fintechlabs.testframework.condition.as;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public class ClearClientAuthentication extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		// remove the client auth from the environment
		env.removeNativeValue("client_authentication_success");
		env.removeObject("client_authentication");

		return env;

	}

}
