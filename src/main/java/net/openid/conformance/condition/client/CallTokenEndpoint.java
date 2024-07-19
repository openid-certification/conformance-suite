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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

/**
 * CallTokenEndpoint is effectively deprecated, new code is encouraged to use CallTokenEndpointAndReturnFullResponse,
 * which allows the test to verify the exact http status code and headers.
 *
 * TODO: this should probably be renamed 'CallTokenEndpointExpectingSuccess' to differentiate it from
 * CallTokenEndpointAndReturnFullResponse.
 */
public class CallTokenEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "server", "token_endpoint_request_form_parameters" })
	@PostEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		String tokenEndpoint = env.getString("token_endpoint") != null ? env.getString("token_endpoint") : env.getString("server", "token_endpoint");

		if (Strings.isNullOrEmpty(tokenEndpoint)) {
			throw error("Couldn't find token endpoint");
		}

		if (!env.containsObject("token_endpoint_request_form_parameters")) {
			throw error("Couldn't find request form");
		}

		// build up the form
		JsonObject formJson = env.getObject("token_endpoint_request_form_parameters");
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		for (String key : formJson.keySet()) {
			form.add(key, OIDFJSON.getString(formJson.get(key)));
		}

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			// extract the headers for use (below)
			HttpHeaders headers = headersFromJson(env.getObject("token_endpoint_request_headers"));

			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

			String jsonString = null;

			try {
				jsonString = restTemplate.postForObject(tokenEndpoint, request, String.class);
			} catch (RestClientResponseException e) {

				throw error("Error from the token endpoint", args("code", e.getStatusCode().value(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			} catch (RestClientException e) {
				String msg = "Call to token endpoint " + tokenEndpoint + " failed";
				if (e.getCause() != null) {
					msg += " - " +e.getCause().getMessage();
				}
				throw error(msg, e);
			}

			if (Strings.isNullOrEmpty(jsonString)) {
				throw error("Didn't get back a response from the token endpoint");
			} else {
				try {
					JsonElement jsonRoot = JsonParser.parseString(jsonString);
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
			throw error("Error creating HTTP Client", e);
		}

	}

}
