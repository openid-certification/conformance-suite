package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

/**
 * General utility class for calling OAuth endpoints
 *
 * This is defined as an endpoint with similar behaviour to the token endpoint, which is the case for most
 * OAuth endpoints that need client authentication - they make a POST request with form encoded contents
 * and expect a JSON response.
 */
public abstract class AbstractCallOAuthEndpoint extends AbstractCondition {
	protected boolean jsonObjectError = false;
	protected boolean jsonParseError = false;
	protected JsonParseException jsonParseException = null;
	protected String endpointName;
	protected String responseEnvironmentKey;

	protected Environment callOAuthEndpoint(Environment env, ResponseErrorHandler errorHandler, String requestFormParametersEnvKey, String requestHeadersEnvKey, String endpointUri, String endpointName, String responseEnvironmentKey) {
		this.endpointName = endpointUri;
		this.responseEnvironmentKey = responseEnvironmentKey;

		JsonObject formJson = env.getObject(requestFormParametersEnvKey);
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		for (String key : formJson.keySet()) {
			JsonElement json = formJson.get(key);
			if (json.isJsonObject()) {
				// presentation submission etc are objects
				form.add(key, json.toString());
			} else {
				form.add(key, OIDFJSON.getString(formJson.get(key)));
			}
		}

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			if (null != errorHandler) {
				restTemplate.setErrorHandler(errorHandler);
			}

			HttpHeaders headers = headersFromJson(requestHeadersEnvKey != null ? env.getObject(requestHeadersEnvKey) : null);

			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

			String jsonString = null;

			try {
				ResponseEntity<String> response = restTemplate
					.exchange(endpointUri, HttpMethod.POST, request, String.class);

				jsonString = response.getBody();
				addFullResponse(env, response);

			} catch (RestClientResponseException e) {
				return handleRestClientResponseException(env, e);
			} catch (RestClientException e) {
				return handleClientException(env, e);
			}

			if (Strings.isNullOrEmpty(jsonString)) {
				throw error("Missing or empty response from the " + endpointName);
			}

			if (jsonParseError && (null != jsonParseException)) {
				return handleJsonParseException(env, jsonParseException);
			}
			if (jsonObjectError) {
				throw error(endpointName + " did not return a JSON object", args("response", jsonString));
			}
			logSuccess("Parsed " + endpointName + " response", env.getObject(responseEnvironmentKey));
			return env;
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException |
				 KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}
	}

	protected void addFullResponse(Environment env, ResponseEntity<String> response) {
		JsonObject fullResponse = convertJsonResponseForEnvironment(endpointName, response, true);
		env.putObject(responseEnvironmentKey, fullResponse);
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
			throw error("Empty response from the " + endpointName + " endpoint");
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
			throw error("Response from " + endpointName + " endpoint does not appear to be JSON.", e,
				args("response", jsonString));
		}

		return responseInfo;
	}

	protected Environment handleJsonParseException(Environment env, JsonParseException e) {
		throw error("Error parsing "+endpointName+" response body as JSON", e);
	}

	protected Environment handleRestClientResponseException(Environment env, RestClientResponseException e) {
		throw error("RestClientResponseException occurred whilst calling "+endpointName,
			args("code", e.getStatusCode().value(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
	}

	protected Environment handleClientException(Environment env, RestClientException e) {
		String msg = "Call to "+endpointName+" failed";
		if (e.getCause() != null) {
			msg += " - " + e.getCause().getMessage();
		}
		throw error(msg, e);
	}
}
