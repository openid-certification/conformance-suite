package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExpectServerDoesNotCallNotificationEndpointTwice extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "times_server_called_notification_endpoint")
	public Environment evaluate(Environment in) {
		String calledTimes = in.getString("times_server_called_notification_endpoint");

		int times;
		if (calledTimes == null) {
			times = 1;
		} else {
			times = Integer.valueOf(calledTimes) + 1;
		}

		in.putString("times_server_called_notification_endpoint", String.valueOf(times));

		if (times > 1) {
			throw error(String.format("Server called notification endpoint %d times while it shouldn't.", times));
		}

		logSuccess("Server called notification endpoint once");

		return in;
	}
}
