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

import org.apache.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.openbanking.FAPIOBGetResourceEndpoint;
import io.fintechlabs.testframework.openbanking.FAPIOBGetResourceEndpoint.Endpoint;
import io.fintechlabs.testframework.testmodule.Environment;

public class DisallowAccessTokenInQuery extends AbstractCondition {

	private static final String ACCOUNTS_RESOURCE = "accounts";

	@Override
	@PreEnvironment(required = { "access_token", "resource" })
	public Environment evaluate(Environment env) {

		String accessToken = env.getString("access_token", "value");
		if (Strings.isNullOrEmpty(accessToken)) {
			throw error("Access token not found");
		}

		String resourceEndpoint = FAPIOBGetResourceEndpoint.getBaseResourceURL(env, Endpoint.ACCOUNTS_RESOURCE);
		if (Strings.isNullOrEmpty(resourceEndpoint)) {
			throw error("Resource endpoint not found");
		}

		// Build the endpoint URL
		String accountRequestsUrl = UriComponentsBuilder.fromUriString(resourceEndpoint)
			.path(ACCOUNTS_RESOURCE)
			.toUriString();

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpHeaders headers = new HttpHeaders();

			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
			headers.setAcceptCharset(Collections.singletonList(Charset.forName("UTF-8")));

			HttpEntity<?> request = new HttpEntity<>(headers);

			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(accountRequestsUrl);
			builder.queryParam("access_token", accessToken);

			ResponseEntity<String> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, request, String.class);

			throw error("Got a successful response from the resource endpoint", args("body", response.getBody()));
		} catch (RestClientResponseException e) {
			if (e.getRawStatusCode() == HttpStatus.SC_BAD_REQUEST ||
				e.getRawStatusCode() == HttpStatus.SC_UNAUTHORIZED ||
				e.getRawStatusCode() == HttpStatus.SC_REQUEST_URI_TOO_LONG) {
				logSuccess("Resource server refused request", args("code", e.getRawStatusCode(), "status", e.getStatusText()));
				return env;
			} else {
				throw error("Error from the resource endpoint", e, args("code", e.getRawStatusCode(), "status", e.getStatusText()));
			}
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		}
	}

}
