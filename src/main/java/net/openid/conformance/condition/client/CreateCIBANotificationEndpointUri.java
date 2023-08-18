package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateCIBANotificationEndpointUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "notification_uri")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		// see https://gitlab.com/openid/conformance-suite/wikis/Developers/Build-&-Run#ciba-notification-endpoint
		String externalUrlOverride = env.getString("external_url_override");
		if (!Strings.isNullOrEmpty(externalUrlOverride)) {
			baseUrl = externalUrlOverride;
		}

		// calculate the redirect URI based on our given base URL
		String notificationUri = baseUrl + "/ciba-notification-endpoint";
		env.putString("notification_uri", notificationUri);

		logSuccess("Created ciba notification endpoint URI",
			args("notification_uri", notificationUri));

		return env;
	}

}
