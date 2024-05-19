package net.openid.conformance.condition.client;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
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
import java.util.Collections;

public class CallTokenEndpointAndReturnFullResponse extends AbstractCondition {
	protected boolean jsonObjectError = false;
	protected boolean jsonParseError = false;
	protected JsonParseException jsonParseException = null;

	@Override
	@PreEnvironment(required = { "server", "token_endpoint_request_form_parameters" })
	@PostEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		return callTokenEndpoint(env, new DefaultResponseErrorHandler() {
			@Override
			public boolean hasError(ClientHttpResponse response) throws IOException {
				// Treat all http status codes as 'not an error', so spring never throws an exception due to the http
				// status code meaning the rest of our code can handle http status codes how it likes
				return false;
			}
		});
	}


	public Environment callTokenEndpoint(Environment env, ResponseErrorHandler errorHandler) {

		// build up the form
		JsonObject formJson = env.getObject("token_endpoint_request_form_parameters");
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		for (String key : formJson.keySet()) {
			form.add(key, OIDFJSON.getString(formJson.get(key)));
		}

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			if(null != errorHandler) {
				restTemplate.setErrorHandler(errorHandler);
			}

			HttpHeaders headers = headersFromJson(env.getObject("token_endpoint_request_headers"));

			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

			String jsonString = null;

			try {
				final String tokenEndpointUri = env.getString("token_endpoint") != null ? env.getString("token_endpoint") : env.getString("server", "token_endpoint");

				ResponseEntity<String> response = restTemplate
					.exchange(tokenEndpointUri, HttpMethod.POST, request, String.class);

				jsonString = response.getBody();
				addFullResponse(env, response);

			} catch (RestClientResponseException e) {
				return handleRestClientResponseException(env, e);
			} catch (RestClientException e) {
				return handleClientException(env, e);
			}

			if (Strings.isNullOrEmpty(jsonString)) {
				throw error("Missing or empty response from the token endpoint");
			}

			if(jsonParseError && (null != jsonParseException)) {
				return handleJsonParseException(env, jsonParseException);
			}
			if(jsonObjectError) {
				throw error("Token Endpoint did not return a JSON object", args("response", jsonString));
			}
			env.putObject("token_endpoint_response", env.getElementFromObject("token_endpoint_response_full", "body_json").getAsJsonObject());
			logSuccess("Parsed token endpoint response", env.getObject("token_endpoint_response"));
			return env;
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}

	}


	protected void addFullResponse(Environment env, ResponseEntity<String> response) {
		env.putInteger("token_endpoint_response_http_status", response.getStatusCode().value());

		JsonObject responseHeaders = mapToJsonObject(response.getHeaders(), true);

		env.putObject("token_endpoint_response_headers", responseHeaders);

		JsonObject fullResponse = convertJsonResponseForEnvironment("token", response, true);
		env.putObject("token_endpoint_response_full", fullResponse);
	}

	@Override
	protected JsonObject convertJsonResponseForEnvironment(String endpointName, ResponseEntity<String> response, boolean allowParseFailure) {
		jsonParseError = false;
		jsonObjectError = false;
		jsonParseException = null;

		JsonObject responseInfo = convertResponseForEnvironment(endpointName, response);

		String jsonString = response.getBody();
		if (Strings.isNullOrEmpty(jsonString)) {
			if (allowParseFailure) {
				return responseInfo;
			}
			throw error("Empty response from the "+endpointName+" endpoint");
		}

		try {
			JsonElement jsonRoot = JsonParser.parseString(jsonString);
			if (jsonRoot == null || !jsonRoot.isJsonObject()) {
				if (allowParseFailure) {
					jsonObjectError = true;
					return responseInfo;
				}

				throw error(endpointName + " endpoint did not return a JSON object.",
					args("response", jsonString));
			}

			JsonObject bodyJson = jsonRoot.getAsJsonObject();

			responseInfo.add("body_json", bodyJson);

		} catch (JsonParseException e) {
			if (allowParseFailure) {
				jsonParseError = true;
				jsonParseException = e; // save exception for later
				return responseInfo;
			}
			throw error("Response from "+endpointName+" endpoint does not appear to be JSON.", e,
				args("response", jsonString));
		}

		return responseInfo;
	}


	protected Environment handleJsonParseException(Environment env, JsonParseException e) {
		throw error("Error parsing token endpoint response body as JSON", e);
	}

	protected Environment handleRestClientResponseException(Environment env, RestClientResponseException e) {
		throw error("RestClientResponseException occurred whilst calling token endpoint",
			args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
	}

	protected Environment handleClientException(Environment env, RestClientException e) {
		String msg = "Call to token endpoint failed";
		if (e.getCause() != null) {
			msg += " - " + e.getCause().getMessage();
		}
		throw error(msg, e);
	}
}
