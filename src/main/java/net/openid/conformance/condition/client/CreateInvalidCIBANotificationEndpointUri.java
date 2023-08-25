package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateInvalidCIBANotificationEndpointUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "invalid_notification_uri")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		String invalidRedirectionUri = baseUrl + "/invalid-ciba-notification-endpoint";
		env.putString("invalid_notification_uri", invalidRedirectionUri);

		logSuccess("Created invalid ciba notification endpoint URI",
			args("invalid_notification_uri", invalidRedirectionUri));

		return env;
	}
}
