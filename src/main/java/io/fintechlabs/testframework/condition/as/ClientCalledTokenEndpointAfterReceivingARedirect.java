package io.fintechlabs.testframework.condition.as;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public class ClientCalledTokenEndpointAfterReceivingARedirect extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		logFailure("Server has incorrectly called ciba_notification_endpoint after following a HTTP 301 Redirect from the client.");

		return env;
	}

}
