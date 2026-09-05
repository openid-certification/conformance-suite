package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import javax.net.ssl.SSLException;

public class PingClientNotificationEndpointWithoutMTLS extends PingClientNotificationEndpointExpectingClientError {
	private static final String CLIENT_REJECTED_NOTIFICATION_WITHOUT_MTLS =
		"client_rejected_notification_without_mtls";

	@Override
	@PreEnvironment(required = { "client", "mutual_tls_authentication" },
		strings = { "auth_req_id", "client_notification_token" })
	public Environment evaluate(Environment env) {
		env.removeNativeValue(CLIENT_REJECTED_NOTIFICATION_WITHOUT_MTLS);
		try {
			Environment result = super.evaluate(env);
			if (!Boolean.TRUE.equals(env.getBoolean(CLIENT_REJECTED_NOTIFICATION_WITHOUT_MTLS))) {
				throw error("Client notification endpoint accepted a request without a mutual TLS client certificate");
			}
			return result;
		} finally {
			env.removeNativeValue(CLIENT_REJECTED_NOTIFICATION_WITHOUT_MTLS);
		}
	}

	@Override
	protected boolean useMtlsForHttpRequests() {
		// HTTP I/O releases the test lock. Keep credentials available to concurrent requests.
		return false;
	}

	@Override
	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {
		if (e.getStatusCode().is4xxClientError()) {
			env.putBoolean(CLIENT_REJECTED_NOTIFICATION_WITHOUT_MTLS, true);
		}
		return super.handleClientResponseException(env, e);
	}

	@Override
	protected Environment handleClientException(Environment env, RestClientException e) {
		if (!hasSslException(e)) {
			return super.handleClientException(env, e);
		}
		env.putBoolean(CLIENT_REJECTED_NOTIFICATION_WITHOUT_MTLS, true);
		logSuccess("Client notification endpoint rejected a request without a mutual TLS client certificate");
		return env;
	}

	private static boolean hasSslException(Throwable throwable) {
		for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
			if (cause instanceof SSLException) {
				return true;
			}
		}
		return false;
	}
}
