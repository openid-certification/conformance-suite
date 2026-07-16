package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.Environment;
import org.springframework.web.client.RestClientResponseException;

public abstract class PingClientNotificationEndpointExpectingClientError extends PingClientNotificationEndpoint {

	@Override
	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {
		if (!e.getStatusCode().is4xxClientError()) {
			return super.handleClientResponseException(env, e);
		}
		env.putInteger("client_notification_endpoint_response_http_status", e.getStatusCode().value());
		return env;
	}
}
