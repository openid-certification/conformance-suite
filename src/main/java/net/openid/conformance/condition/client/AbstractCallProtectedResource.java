package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.util.Collections;

public abstract class AbstractCallProtectedResource extends AbstractCondition {

	protected String getAccessToken(Environment env) {

		String accessToken = env.getString("access_token", "value");
		if (Strings.isNullOrEmpty(accessToken)) {
			throw error("Access token not found");
		}

		String tokenType = env.getString("access_token", "type");
		if (Strings.isNullOrEmpty(tokenType)) {
			throw error("Token type not found");
		} else if (!tokenType.equalsIgnoreCase("Bearer")) {
			throw error("Access token is not a bearer token", args("token_type", tokenType));
		}

		return accessToken;
	}

	protected String getUri(Environment env) {

		String resourceUri = env.getString("protected_resource_url");

		if (Strings.isNullOrEmpty(resourceUri)){
			throw error("Missing Resource URL");
		}

		return resourceUri;
	}

	protected HttpMethod getMethod(Environment env) {

		HttpMethod resourceMethod = HttpMethod.GET;
		String configuredMethod = env.getString("resource", "resourceMethod");
		if (!Strings.isNullOrEmpty(configuredMethod)) {
			resourceMethod = HttpMethod.valueOf(configuredMethod);
		}

		return resourceMethod;
	}

	protected HttpHeaders getHeaders(Environment env) {

		return new HttpHeaders();
	}

	protected MediaType getMediaType(Environment env) {

		return MediaType.APPLICATION_FORM_URLENCODED;
	}

	protected Object getBody(Environment env) {
		String requestEntity = env.getString("resource_request_entity");
		return requestEntity;
	}

	protected Environment callProtectedResource(Environment env) {
		String uri = getUri(env);

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpMethod method = getMethod(env);
			HttpHeaders headers = getHeaders(env);

			if (headers.getAccept().isEmpty()) {
				headers.setAccept(Collections.singletonList(DATAUTILS_MEDIATYPE_APPLICATION_JSON_UTF8));
				headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
			}

			if (method == HttpMethod.POST && headers.getContentType() == null) {
				// See https://bitbucket.org/openid/connect/issues/1137/is-content-type-application-x-www-form
				headers.setContentType(getMediaType(env));
			}

			HttpEntity<?> request = new HttpEntity<>(getBody(env), headers);

			ResponseEntity<String> response = restTemplate.exchange(uri, method, request, String.class);
			JsonObject responseCode = new JsonObject();
			responseCode.addProperty("code", response.getStatusCodeValue());
			String responseBody = response.getBody();
			JsonObject responseHeaders = mapToJsonObject(response.getHeaders(), true);
			JsonObject fullResponse = convertResponseForEnvironment("resource", response);

			return handleClientResponse(env, responseCode, responseBody, responseHeaders, fullResponse);
		} catch (RestClientResponseException e) {
			return handleClientResponseException(env, e);
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (RestClientException e) {
			String msg = "Call to protected resource " + uri + " failed";
			if (e.getCause() != null) {
				msg += " - " +e.getCause().getMessage();
			}
			throw error(msg, e);
		}
	}

	protected abstract Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse);

	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {
		throw error("Unexpected error from the resource endpoint", args("code", e.getRawStatusCode(), "status", e.getStatusText()));
	}
}
