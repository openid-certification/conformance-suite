package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import javax.net.ssl.SSLException;

public class PingClientNotificationEndpointWithoutMTLS extends PingClientNotificationEndpointExpectingClientError {
	private static final String CLIENT_REJECTED_NOTIFICATION_WITHOUT_MTLS =
		"client_rejected_notification_without_mtls";

	@Override
	@PreEnvironment(required = "client", strings = { "auth_req_id", "client_notification_token" })
	public Environment evaluate(Environment env) {
		JsonObject mtlsCredentials = env.getObject("mutual_tls_authentication");
		if (mtlsCredentials == null) {
			throw error("Mutual TLS credentials were not loaded before testing the client notification endpoint");
		}

		env.removeObject("mutual_tls_authentication");
		env.removeNativeValue(CLIENT_REJECTED_NOTIFICATION_WITHOUT_MTLS);
		try {
			Environment result = super.evaluate(env);
			if (!Boolean.TRUE.equals(env.getBoolean(CLIENT_REJECTED_NOTIFICATION_WITHOUT_MTLS))) {
				throw error("Client notification endpoint accepted a request without a mutual TLS client certificate");
			}
			return result;
		} finally {
			env.putObject("mutual_tls_authentication", mtlsCredentials);
			env.removeNativeValue(CLIENT_REJECTED_NOTIFICATION_WITHOUT_MTLS);
		}
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
