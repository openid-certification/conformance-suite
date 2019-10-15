package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ServerCalledInvalidNotificationEndpoint extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		throw error("Server has incorrectly called invalid_ciba_notification_endpoint after following a redirect request with 301 http status code from the client while it shouldn't.");

	}

}
