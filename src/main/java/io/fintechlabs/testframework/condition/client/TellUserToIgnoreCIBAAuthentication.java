package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public class TellUserToIgnoreCIBAAuthentication extends AbstractCondition {

	public Environment evaluate(Environment env) {

		log("Waiting for CIBA authorization to time out. If the AS respects 'requested_expiry' this will happen within 30 seconds.");

		return env;

	}

}
