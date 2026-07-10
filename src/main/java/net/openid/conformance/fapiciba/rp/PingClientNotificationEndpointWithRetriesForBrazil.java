package net.openid.conformance.fapiciba.rp;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class PingClientNotificationEndpointWithRetriesForBrazil extends PingClientNotificationEndpoint {

	private static final int MAXIMUM_ATTEMPTS = 3;

	@Override
	protected int getMaximumAttempts() {
		return MAXIMUM_ATTEMPTS;
	}

	@Override
	protected boolean shouldRetry(RestClientException e) {
		if (!(e instanceof RestClientResponseException responseException)) {
			return true;
		}

		int statusCode = responseException.getStatusCode().value();
		return responseException.getStatusCode().is5xxServerError()
			|| statusCode == HttpStatus.REQUEST_TIMEOUT.value()
			|| statusCode == HttpStatus.TOO_MANY_REQUESTS.value();
	}
}
