package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketException;
import java.net.SocketTimeoutException;

public class PingClientNotificationEndpointWithRetriesForBrazil extends PingClientNotificationEndpoint {

	private static final int MAXIMUM_ATTEMPTS = 3;

	@Override
	protected int getMaximumAttempts() {
		return MAXIMUM_ATTEMPTS;
	}

	@Override
	protected void markPingAttemptStarted(Environment env) {
		env.putBoolean(CLIENT_PING_ATTEMPTED, true);
	}

	@Override
	protected boolean shouldRetry(RestClientException e) {
		if (e instanceof RestClientResponseException responseException) {
			int statusCode = responseException.getStatusCode().value();
			return responseException.getStatusCode().is5xxServerError()
				|| statusCode == HttpStatus.REQUEST_TIMEOUT.value()
				|| statusCode == HttpStatus.TOO_MANY_REQUESTS.value();
		}

		for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
			if (cause instanceof SocketException || cause instanceof SocketTimeoutException) {
				return true;
			}
		}
		return false;
	}
}
