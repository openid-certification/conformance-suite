package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.Environment;
import org.springframework.web.client.RestClientResponseException;

public class PingClientNotificationEndpointWithBadBearerToken extends PingClientNotificationEndpoint {

	@Override
	protected String getBearerToken(Environment env) {
		return env.getString("client_notification_token") + "1";
	}

	@Override
	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {
		env.putInteger("client_notification_endpoint_response_http_status", e.getStatusCode().value());
		env.putBoolean("client_was_pinged", true);

		return env;
	}
}
