package io.fintechlabs.testframework.condition.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

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

	protected Environment callProtectedResource(Environment env) {

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpHeaders headers = getHeaders(env);

			if (headers.getAccept().isEmpty()) {
				headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
				headers.setAcceptCharset(Collections.singletonList(Charset.forName("UTF-8")));
			}

			HttpEntity<?> request = new HttpEntity<>(headers);

			ResponseEntity<String> response = restTemplate.exchange(getUri(env), getMethod(env), request, String.class);
			JsonObject responseCode = new JsonObject();
			responseCode.addProperty("code", response.getStatusCodeValue());
			String responseBody = response.getBody();
			JsonObject responseHeaders = mapToJsonObject(response.getHeaders(), true);

			return handleClientResponse(env, responseCode, responseBody, responseHeaders);
		} catch (RestClientResponseException e) {
			return handleClientResponseException(env, e);
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		}
	}

	protected abstract Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders);

	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {
		throw error("Unexpected error from the resource endpoint", e, args("code", e.getRawStatusCode(), "status", e.getStatusText()));
	}
}
