package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public class TellUserToRejectCIBAAuthentication extends AbstractCondition {

	public Environment evaluate(Environment env) {

		log("Please reject/cancel the authentication request");

		return env;

	}

}
