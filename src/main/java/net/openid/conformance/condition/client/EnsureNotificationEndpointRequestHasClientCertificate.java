package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureNotificationEndpointRequestHasClientCertificate extends AbstractCondition {

	@Override
	@PreEnvironment(required = "notification_callback")
	public Environment evaluate(Environment env) {
		String certificate = env.getString("notification_callback", "headers.x-ssl-cert");
		// The TLS proxy supplies this header. This checks certificate presence, not certificate trust
		// or binding to an authorization server identity; those require separate validation.
		if (certificate == null || certificate.isBlank() || "(null)".equals(certificate)) {
			throw error("Client notification endpoint request did not include a mutual TLS client certificate");
		}

		logSuccess("Client notification endpoint request included a mutual TLS client certificate");
		return env;
	}
}
