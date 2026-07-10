package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureNotificationEndpointWasRetried extends AbstractCondition {

	@Override
	@PreEnvironment(integers = "notification_endpoint_call_count")
	public Environment evaluate(Environment env) {
		int callCount = env.getInteger("notification_endpoint_call_count");

		if (callCount < 2) {
			throw error("The authorization server did not retry the ping notification after a transient endpoint failure",
				args("notification_endpoint_call_count", callCount));
		}

		logSuccess("The authorization server retried the ping notification after a transient endpoint failure",
			args("notification_endpoint_call_count", callCount));
		return env;
	}
}
