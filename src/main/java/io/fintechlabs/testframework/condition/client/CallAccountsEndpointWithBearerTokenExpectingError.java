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

import io.fintechlabs.testframework.testmodule.OIDFJSON;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.openbanking.FAPIOBGetResourceEndpoint;
import io.fintechlabs.testframework.openbanking.FAPIOBGetResourceEndpoint.Endpoint;
import io.fintechlabs.testframework.testmodule.Environment;


public class CallAccountsEndpointWithBearerTokenExpectingError extends AbstractCondition {

	private static final String ACCOUNTS_RESOURCE = "accounts";

	public CallAccountsEndpointWithBearerTokenExpectingError(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = { "access_token", "resource" })
	@PostEnvironment(required = "resource_endpoint_response_headers", strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {

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

		String resourceEndpoint = FAPIOBGetResourceEndpoint.getBaseResourceURL(env, Endpoint.ACCOUNTS_RESOURCE);
		if (Strings.isNullOrEmpty(resourceEndpoint)) {
			throw error("Resource endpoint not found");
		}

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		// Build the endpoint URL
		String accountRequestsUrl = UriComponentsBuilder.fromUriString(resourceEndpoint)
			.path(ACCOUNTS_RESOURCE)
			.toUriString();

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpHeaders headers = headersFromJson(requestHeaders);

			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
			headers.setAcceptCharset(Collections.singletonList(Charset.forName("UTF-8")));
			headers.set("Authorization", String.join(" ", "Bearer", accessToken));

			HttpEntity<?> request = new HttpEntity<>(headers);

			ResponseEntity<String> response = null;

			try {
				response = restTemplate.exchange(accountRequestsUrl, HttpMethod.GET, request, String.class);
			} catch (RestClientResponseException e) {

				logSuccess("Resource endpoint returned error", args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));

				return env;
			}

			String responseBody = response.getBody();

			if (Strings.isNullOrEmpty(responseBody)) {
				throw error("Empty response from the resource endpoint");
			} else {
				try {
					JsonElement jsonRoot = new JsonParser().parse(responseBody);
					if (jsonRoot == null || !jsonRoot.isJsonObject()) {
						throw error("Resource endpoint did not return a JSON object");
					}

					JsonObject responseObj = jsonRoot.getAsJsonObject();

					if (responseObj.has("error") && !Strings.isNullOrEmpty(OIDFJSON.getString(responseObj.get("error")))) {
						logSuccess("Found error in resource endpoint error response", responseObj);
						return env;
					} else {
						throw error("No error from resource endpoint", responseObj);
					}

				} catch (JsonParseException e) {
					throw error(e);
				}
			}
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		}
	}

}
