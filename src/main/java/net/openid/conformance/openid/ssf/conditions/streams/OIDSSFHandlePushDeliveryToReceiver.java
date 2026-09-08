package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.condition.client.AbstractCallEndpoint;
import net.openid.conformance.openid.federation.IgnoreErrorsErrorHandler;
import net.openid.conformance.openid.ssf.SsfConstants;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.function.BiConsumer;

public class OIDSSFHandlePushDeliveryToReceiver extends AbstractCallEndpoint {

	private final String streamId;
	private final OIDSSFSecurityEvent event;
	private final BiConsumer<String, OIDSSFSecurityEvent> onSuccess;
	private final BiConsumer<String, OIDSSFSecurityEvent> onNotAcknowledged;

	public OIDSSFHandlePushDeliveryToReceiver(String streamId, OIDSSFSecurityEvent event, BiConsumer<String, OIDSSFSecurityEvent> onSuccess) {
		this(streamId, event, onSuccess, (sid, ev) -> {
		});
	}

	public OIDSSFHandlePushDeliveryToReceiver(String streamId, OIDSSFSecurityEvent event,
		BiConsumer<String, OIDSSFSecurityEvent> onSuccess,
		BiConsumer<String, OIDSSFSecurityEvent> onNotAcknowledged) {
		this.streamId = streamId;
		this.event = event;
		this.onSuccess = onSuccess;
		this.onNotAcknowledged = onNotAcknowledged;
	}

	@Override
	public Environment evaluate(Environment env) {

		String endpointUri = env.getString("ssf", "streams." + streamId + ".delivery.endpoint_url");
		String authHeader = env.getString("ssf", "streams." + streamId + ".delivery.authorization_header");

		this.endpointName = "receiver push endpoint";
		this.responseEnvironmentKey = "endpoint_response";

		// Remove any response left over from an earlier delivery so follow-up checks
		// (e.g. the RFC 8935 2.2 202 check) can never run against a stale response
		// when this delivery fails without producing one.
		env.removeObject(responseEnvironmentKey);

		log("Call " + endpointName + " for stream_id=" + streamId + " for event " + event.type() + " jti=" + event.jti(),
			args("stream_id", streamId, "push_endpoint", endpointUri, "jti", event.jti(), "event_type", event.type()));

		try {
			RestTemplate restTemplate = createRestTemplate(env);
			restTemplate.setErrorHandler(new IgnoreErrorsErrorHandler());

			HttpHeaders headers = createHeaders(authHeader);

			HttpEntity<String> request = new HttpEntity<>(event.securityEventToken(), headers);
			try {
				ResponseEntity<String> response = restTemplate.exchange(endpointUri, HttpMethod.POST, request, String.class);
				addFullResponse(env, response);
			} catch (RestClientResponseException e) {
				return handleRestClientResponseException(env, e);
			} catch (RestClientException e) {
				return handleClientException(env, e);
			}

			int status = env.getInteger(responseEnvironmentKey, "status");
			if (status >= 200 && status < 300) {
				logSuccess("Got " + endpointName + " response", env.getObject(responseEnvironmentKey));
				// RFC 8935 2.2: the receiver acknowledges successful transmission with 202.
				// Only a success response counts as delivered-and-acknowledged; the strict
				// 202 status check runs separately in the caller.
				onSuccess.accept(streamId, event);
			} else {
				// RFC 8935 2.3: an error response means the receiver rejected the SET -
				// it must NOT be recorded as a successful delivery/acknowledgement.
				log("Receiver answered the push delivery with an error status; not treating the SET as acknowledged",
					args("status", status, "jti", event.jti(), "event_type", event.type(),
						"response", env.getObject(responseEnvironmentKey)));
				onNotAcknowledged.accept(streamId, event);
			}
			return env;
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException |
				 KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}
	}

	/**
	 * When the push call fails without an HTTP response, leave a synthetic
	 * {@code endpoint_response} (status 0) so follow-up status checks (the RFC 8935
	 * 2.2 202 check, the invalid-SET rejection check) grade a normal FAILURE
	 * instead of aborting the whole test on a missing pre-environment key.
	 */
	@Override
	protected Environment handleClientException(Environment env, org.springframework.web.client.RestClientException e) {
		env.putObject(responseEnvironmentKey, synthesizeFailedResponse(0));
		onNotAcknowledged.accept(streamId, event);
		return super.handleClientException(env, e);
	}

	@Override
	protected Environment handleRestClientResponseException(Environment env, RestClientResponseException e) {
		// preserve the real HTTP status so the rejection is reported accurately
		env.putObject(responseEnvironmentKey, synthesizeFailedResponse(e.getStatusCode().value()));
		onNotAcknowledged.accept(streamId, event);
		return super.handleRestClientResponseException(env, e);
	}

	private com.google.gson.JsonObject synthesizeFailedResponse(int status) {
		com.google.gson.JsonObject response = new com.google.gson.JsonObject();
		response.addProperty("endpoint_name", endpointName);
		// 0 = no HTTP response was received (connection-level failure)
		response.addProperty("status", status);
		return response;
	}

	protected HttpHeaders createHeaders(String authorizationHeader) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set(HttpHeaders.CONTENT_TYPE, SsfConstants.SECURITY_EVENT_TOKEN_CONTENT_TYPE);

		if (authorizationHeader != null) {
			httpHeaders.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
		}
		return httpHeaders;
	}
}
