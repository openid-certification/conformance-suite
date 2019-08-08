package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CreateInvalidCIBANotificationEndpointUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "invalid_notification_uri")
	public Environment evaluate(Environment in) {
		String baseUrl = in.getString("base_url");

		if (Strings.isNullOrEmpty(baseUrl)) {
			throw error("Base URL was null or empty");
		}

		String invalidRedirectionUri = baseUrl + "/invalid-ciba-notification-endpoint";
		in.putString("invalid_notification_uri", invalidRedirectionUri);

		logSuccess("Created invalid ciba notification endpoint URI",
			args("invalid_notification_uri", invalidRedirectionUri));

		return in;
	}
}
