package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Collections;

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
			headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JSON_UTF8);
			headers.setBearerAuth(env.getString("client_notification_token"));

			HttpEntity<String> request = new HttpEntity<>(pingRequestObject.toString(), headers);

			try {
				String clientNotificationEndpoint = env.getString("client","backchannel_client_notification_endpoint");
				ResponseEntity<String> response = restTemplate.exchange(clientNotificationEndpoint, HttpMethod.POST, request, String.class);

				env.putInteger("client_notification_endpoint_response_http_status", response.getStatusCode().value());

				JsonObject responseHeaders = mapToJsonObject(response.getHeaders(), true);

				logSuccess("Received client notification endpoint response:" + response.getBody());
				return env;

			} catch (RestClientResponseException e) {
				throw error("RestClientResponseException occurred whilst calling token endpoint",
					args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			} catch (RestClientException e) {
				return handleClientException(env, e);
			}
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}
	}

	protected Environment handleClientException(Environment env, RestClientException e) {
		String msg = "Call to client notification endpoint failed";
		if (e.getCause() != null) {
			msg += " - " + e.getCause().getMessage();
		}
		throw error(msg, e);
	}
}
