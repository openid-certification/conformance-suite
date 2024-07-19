package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.DefaultResponseErrorHandler;
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

public class PingClientNotificationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client", strings = { "auth_req_id", "client_notification_token" })
	public Environment evaluate(Environment env) {

		JsonObject pingRequestObject = new JsonObject();
		pingRequestObject.addProperty("auth_req_id", env.getString("auth_req_id"));

		RestTemplate restTemplate = null;
		try {
			restTemplate = createRestTemplate(env, true);
			restTemplate.setErrorHandler(new DefaultResponseErrorHandler());

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setBearerAuth(getBearerToken(env));

			HttpEntity<String> request = new HttpEntity<>(pingRequestObject.toString(), headers);

			try {
				String clientNotificationEndpoint = env.getString("client","backchannel_client_notification_endpoint");
				ResponseEntity<String> response = restTemplate.exchange(clientNotificationEndpoint, HttpMethod.POST, request, String.class);

				env.putInteger("client_notification_endpoint_response_http_status", response.getStatusCode().value());
				env.putBoolean("client_was_pinged", true);

				logSuccess("Received client notification endpoint response:" + response.getBody());
				return env;

			} catch (RestClientResponseException e) {
				return handleClientResponseException(env, e);
			} catch (RestClientException e) {
				return handleClientException(env, e);
			}
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}
	}

	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {
		throw error("RestClientResponseException occurred whilst calling token endpoint",
			args("code", e.getStatusCode().value(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
	}

	protected Environment handleClientException(Environment env, RestClientException e) {
		String msg = "Call to client notification endpoint failed";
		if (e.getCause() != null) {
			msg += " - " + e.getCause().getMessage();
		}
		throw error(msg, e);
	}

	protected String getBearerToken(Environment env) {
		return env.getString("client_notification_token");
	}
}
