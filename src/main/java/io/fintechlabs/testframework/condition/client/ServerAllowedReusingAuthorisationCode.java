package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public class ServerAllowedReusingAuthorisationCode extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		throw error("Server has incorrectly allowed reusing authorisation code while it shouldn't.");

	}

}
