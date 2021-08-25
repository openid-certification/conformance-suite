package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Collections;

public class CallTokenEndpointAndReturnFullResponse extends AbstractCondition {

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

			restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
				@Override
				public boolean hasError(ClientHttpResponse response) throws IOException {
					// Treat all http status codes as 'not an error', so spring never throws an exception due to the http
					// status code meaning the rest of our code can handle http status codes how it likes
					return false;
				}
			});

			HttpHeaders headers = headersFromJson(env.getObject("token_endpoint_request_headers"));

			headers.setAccept(Collections.singletonList(DATAUTILS_MEDIATYPE_APPLICATION_JSON_UTF8));
			headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

			String jsonString = null;

			try {
				final String tokenEndpointUri = env.getString("token_endpoint") != null ? env.getString("token_endpoint") : env.getString("server", "token_endpoint");

				restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
					@Override
					public boolean hasError(ClientHttpResponse response) throws IOException {
						// Treat all http status codes as 'not an error', so spring never throws an exception due to the http
						// status code meaning the rest of our code can handle http status codes how it likes
						return false;
					}
				});

				ResponseEntity<String> response = restTemplate
					.exchange(tokenEndpointUri, HttpMethod.POST, request, String.class);

				env.putInteger("token_endpoint_response_http_status", response.getStatusCode().value());

				JsonObject responseHeaders = mapToJsonObject(response.getHeaders(), true);

				env.putObject("token_endpoint_response_headers", responseHeaders);

				jsonString = response.getBody();

			} catch (RestClientResponseException e) {
				throw error("RestClientResponseException occurred whilst calling token endpoint",
					args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			} catch (RestClientException e) {
				return handleResponseException(env, e);
			}

			if (Strings.isNullOrEmpty(jsonString)) {
				throw error("Missing or empty response from the token endpoint");
			}

			try {
				JsonElement jsonRoot = new JsonParser().parse(jsonString);
				if (jsonRoot == null || !jsonRoot.isJsonObject()) {
					throw error("Token Endpoint did not return a JSON object");
				}

				logSuccess("Parsed token endpoint response", jsonRoot.getAsJsonObject());

				env.putObject("token_endpoint_response", jsonRoot.getAsJsonObject());

				return env;
			} catch (JsonParseException e) {
				return handleJsonParseException(env, e);
			}

		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}

	}

	protected Environment handleJsonParseException(Environment env, JsonParseException e) {
		throw error("Error parsing token endpoint response body as JSON", e);
	}

	protected Environment handleResponseException(Environment env, RestClientException e) {
		String msg = "Call to token endpoint failed";
		if (e.getCause() != null) {
			msg += " - " + e.getCause().getMessage();
		}
		throw error(msg, e);
	}

}
