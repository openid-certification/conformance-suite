package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;

public class PingClientNotificationEndpointWithoutBearerToken extends PingClientNotificationEndpointExpectingClientError {

	@Override
	protected void addAuthorizationHeader(HttpHeaders headers, Environment env) {
		// Deliberately omit the bearer token to verify that the client rejects an unauthenticated notification.
	}
}
