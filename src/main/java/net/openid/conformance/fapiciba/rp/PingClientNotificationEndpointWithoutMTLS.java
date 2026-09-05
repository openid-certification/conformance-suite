package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;

public class PingClientNotificationEndpointWithoutMTLS extends PingClientNotificationEndpointExpectingClientError {
	private static final String CLIENT_REJECTED_NOTIFICATION_WITHOUT_MTLS =
		"client_rejected_notification_without_mtls";

	@Override
	@PreEnvironment(required = { "client", "mutual_tls_authentication" },
		strings = { "auth_req_id", "client_notification_token" })
	public Environment evaluate(Environment env) {
		env.removeNativeValue("notification_without_mtls_rejection");
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
			env.putString("notification_without_mtls_rejection", "http_error");
		}
		return super.handleClientResponseException(env, e);
	}

	@Override
	protected Environment handleClientException(Environment env, RestClientException e) {
		if (!hasSslException(e)) {
			if (hasConnectionClosure(e)) {
				env.putBoolean(CLIENT_REJECTED_NOTIFICATION_WITHOUT_MTLS, true);
				env.putString("notification_without_mtls_rejection", "connection_closed");
				log("Peer closed the notification connection without an HTTP response; certificate rejection is uncertain",
					args("error", e.getMessage()));
				return env;
			}
			return super.handleClientException(env, e);
		}
		env.putBoolean(CLIENT_REJECTED_NOTIFICATION_WITHOUT_MTLS, true);
		env.putString("notification_without_mtls_rejection", "tls_error");
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

	private static boolean hasConnectionClosure(Throwable throwable) {
		for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
			if (cause instanceof NoHttpResponseException
				|| (cause instanceof SocketException
					&& !(cause instanceof ConnectException) && !(cause instanceof NoRouteToHostException))) {
				return true;
			}
		}
		return false;
	}
}
