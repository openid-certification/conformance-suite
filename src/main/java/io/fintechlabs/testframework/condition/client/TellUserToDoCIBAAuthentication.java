package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public class TellUserToDoCIBAAuthentication extends AbstractCondition {

	public Environment evaluate(Environment env) {

		log("Please authenticate and authorize the request");

		return env;

	}

}
