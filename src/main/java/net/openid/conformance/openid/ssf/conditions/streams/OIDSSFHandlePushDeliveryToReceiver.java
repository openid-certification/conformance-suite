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
import java.util.function.Consumer;

public class OIDSSFHandlePushDeliveryToReceiver extends AbstractCallEndpoint {

	private final String streamId;
	private final OIDSSFSecurityEvent event;
	private final Consumer<String> onSuccess;

	public OIDSSFHandlePushDeliveryToReceiver(String streamId, OIDSSFSecurityEvent event, Consumer<String> onSuccess) {
		this.streamId = streamId;
		this.event = event;
		this.onSuccess = onSuccess;
	}

	@Override
	public Environment evaluate(Environment env) {

		String endpointUri = env.getString("ssf", "streams." + streamId + ".delivery.endpoint_url");
		String authHeader = env.getString("ssf", "streams." + streamId + ".delivery.authorization_header");

		this.endpointName = "receiver push endpoint";
		this.responseEnvironmentKey = "push_endpoint_response";

		log("Call " + endpointName + " for stream_id=" + streamId, args("stream_id", streamId, "push_endpoint", endpointUri));

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

			logSuccess("Got " + endpointName + " response", env.getObject(responseEnvironmentKey));

			onSuccess.accept(streamId);
			return env;
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException |
				 KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}
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
