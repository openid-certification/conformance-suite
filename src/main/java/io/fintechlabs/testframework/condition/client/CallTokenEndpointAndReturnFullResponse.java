package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;

public class CallTokenEndpointAndReturnFullResponse extends AbstractCondition {

	private class OurErrorHandler extends DefaultResponseErrorHandler {
		@Override
		public boolean hasError(ClientHttpResponse response) throws IOException {
			// Treat all http status codes as 'not an error', so spring never throws an exception due to the http
			// status code meaning the rest of our code can handle http status codes how it likes
			return false;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(CallTokenEndpointAndReturnFullResponse.class);

	@Override
	@PreEnvironment(required = { "server", "token_endpoint_request_form_parameters" })
	@PostEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		// build up the form
		JsonObject formJson = env.getObject("token_endpoint_request_form_parameters");
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		for (String key : formJson.keySet()) {
			form.add(key, OIDFJSON.getString(formJson.get(key)));
		}

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpHeaders headers = headersFromJson(env.getObject("token_endpoint_request_headers"));

			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
			headers.setAcceptCharset(Collections.singletonList(Charset.forName("UTF-8")));

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

			String jsonString = null;

			try {
				final String tokenEndpointUri = env.getString("server", "token_endpoint");

				restTemplate.setErrorHandler(new OurErrorHandler());

				ResponseEntity<String> response = restTemplate
					.exchange(tokenEndpointUri, HttpMethod.POST, request, String.class);

				env.putInteger("token_endpoint_response_http_status", response.getStatusCode().value());

				JsonObject responseHeaders = mapToJsonObject(response.getHeaders(), true);

				env.putObject("token_endpoint_response_headers", responseHeaders);

				jsonString = response.getBody();

			} catch (RestClientResponseException e) {
				throw error("RestClientResponseException occurred whilst calling token endpoint",
					e, args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			} catch (RestClientException e) {
				return handleResponseException(env, e);
			}

			if (Strings.isNullOrEmpty(jsonString)) {
				throw error("Missing or empty response from the token endpoint");
			} else {
				try {
					JsonElement jsonRoot = new JsonParser().parse(jsonString);
					if (jsonRoot == null || !jsonRoot.isJsonObject()) {
						throw error("Token Endpoint did not return a JSON object");
					}

					logSuccess("Parsed token endpoint response", jsonRoot.getAsJsonObject());

					env.putObject("token_endpoint_response", jsonRoot.getAsJsonObject());

					return env;
				} catch (JsonParseException e) {
					throw error(e);
				}
			}
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			logger.warn("Error creating HTTP Client", e);
			throw error("Error creating HTTP Client", e);
		}

	}

	protected Environment handleResponseException(Environment env, RestClientException e) {
		throw error("RestClientException happened whilst calling token endpoint", ex(e));
	}

}
