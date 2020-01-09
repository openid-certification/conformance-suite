package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateInvalidCIBANotificationEndpointUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "invalid_notification_uri")
	public Environment evaluate(Environment in) {
		String baseUrl = in.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		String invalidRedirectionUri = baseUrl + "/invalid-ciba-notification-endpoint";
		in.putString("invalid_notification_uri", invalidRedirectionUri);

		logSuccess("Created invalid ciba notification endpoint URI",
			args("invalid_notification_uri", invalidRedirectionUri));

		return in;
	}
}
