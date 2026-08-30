package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.Environment;
import org.springframework.web.client.RestClientResponseException;

public class PingClientNotificationEndpointAllowingHttpErrorResponse extends PingClientNotificationEndpoint {

	@Override
	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {
		env.putInteger("client_notification_endpoint_response_http_status", e.getStatusCode().value());
		logSuccess("Received HTTP error response from client notification endpoint",
			args("code", e.getStatusCode().value(), "status", e.getStatusText(),
				"body", e.getResponseBodyAsString()));
		return env;
	}
}
