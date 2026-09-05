package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RejectNonMTLSCIBANotificationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "notification_uri")
	public Environment evaluate(Environment env) {
		throw error("The notification was sent to the regular host. Use the mTLS notification endpoint: " +
			env.getString("notification_uri"), args("notification_uri", env.getString("notification_uri")));
	}
}
