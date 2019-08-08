package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public class ServerCalledInvalidNotificationEndpoint extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		throw error("Server has incorrectly called invalid_ciba_notification_endpoint after following a redirect request with 301 http status code from the client while it shouldn't.");

	}

}
