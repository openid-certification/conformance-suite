package io.fintechlabs.testframework.condition.rs;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public class ClearAccessTokenFromRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		env.removeNativeValue("incoming_access_token");

		return env;

	}

}
