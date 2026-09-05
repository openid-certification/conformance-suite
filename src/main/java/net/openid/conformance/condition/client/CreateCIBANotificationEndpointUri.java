package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateCIBANotificationEndpointUri extends AbstractCondition {

	@Override
	@PreEnvironment
	@PostEnvironment(strings = "notification_uri")
	public Environment evaluate(Environment env) {
		// The required base URL depends on the profile, so validate it here rather than requiring
		// both the regular and mTLS URL unconditionally in @PreEnvironment.
		boolean mtlsRequired = Boolean.TRUE.equals(env.getBoolean("notification_endpoint_requires_mtls"));
		String baseUrl = mtlsRequired ? env.getString("base_mtls_url") : env.getString("base_url");

		if (Strings.isNullOrEmpty(baseUrl)) {
			throw error(mtlsRequired ? "Base mTLS URL is empty" : "Base URL is empty");
		}

		// see https://gitlab.com/openid/conformance-suite/wikis/Developers/Build-&-Run#ciba-notification-endpoint
		// The override only serves /test/, not /test-mtls/. There is no mTLS override; deployments
		// must set fintechlabs.base_mtls_url to their publicly reachable mTLS origin.
		String externalUrlOverride = env.getString("external_url_override");
		if (!mtlsRequired && !Strings.isNullOrEmpty(externalUrlOverride)) {
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
