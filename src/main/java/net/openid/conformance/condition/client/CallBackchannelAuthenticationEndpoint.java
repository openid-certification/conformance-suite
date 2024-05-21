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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;

public class CallBackchannelAuthenticationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "server", "backchannel_authentication_endpoint_request_form_parameters" })
	@PostEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {

		final String endpointKey = "backchannel_authentication_endpoint";
		final String bcAuthEndpoint = env.getString(endpointKey) != null ? env.getString(endpointKey) : env.getString("server", endpointKey);
		if (bcAuthEndpoint == null) {
			throw error("Couldn't find backchannel authentication endpoint");
		}

		// build up the form
		JsonObject formJson = env.getObject("backchannel_authentication_endpoint_request_form_parameters");
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

			HttpHeaders headers = headersFromJson(env.getObject("backchannel_authentication_endpoint_request_headers"));

			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

			String jsonString = null;

			try {

				ResponseEntity<String> response = restTemplate.postForEntity(bcAuthEndpoint, request, String.class);

				JsonObject responseHeaders = mapToJsonObject(response.getHeaders(), true);

				env.putObject("backchannel_authentication_endpoint_response_headers", responseHeaders);

				env.putInteger("backchannel_authentication_endpoint_response_http_status", response.getStatusCode().value());

				jsonString = response.getBody();
			} catch (RestClientResponseException e) {
				throw error("Error from the backchannel authentication endpoint", args("code", e.getStatusCode().value(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			} catch (RestClientException e) {
				String msg = "Call to backchannel authentication endpoint " + bcAuthEndpoint + " failed";
				if (e.getCause() != null) {
					msg += " - " + e.getCause().getMessage();
				}
				throw error(msg, e);
			}

			if (Strings.isNullOrEmpty(jsonString)) {
				throw error("Got an empty response from the backchannel authentication endpoint");
			} else {
				log("Backchannel Authentication endpoint response",
					args("backchannel_authentication_endpoint_response", jsonString));

				try {
					JsonElement jsonRoot = JsonParser.parseString(jsonString);
					if (jsonRoot == null || !jsonRoot.isJsonObject()) {
						throw error("Backchannel Authentication Endpoint did not return a JSON object");
					}

					logSuccess("Parsed backchannel authentication endpoint response", jsonRoot.getAsJsonObject());

					env.putObject("backchannel_authentication_endpoint_response", jsonRoot.getAsJsonObject());

					return env;
				} catch (JsonParseException e) {
					throw error(e);
				}
			}
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}

	}

}
