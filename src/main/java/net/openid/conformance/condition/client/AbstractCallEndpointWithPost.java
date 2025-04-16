package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

/**
 * General utility class for calling endpoints and returning the full response
 */
public abstract class AbstractCallEndpointWithPost extends AbstractCallEndpoint {

	protected Environment callEndpointWithPost(Environment env, ResponseErrorHandler errorHandler, String requestFormParametersEnvKey, String requestHeadersEnvKey, String endpointUri, String endpointName, String responseEnvironmentKey) {
		this.endpointName = endpointUri;
		this.responseEnvironmentKey = responseEnvironmentKey;

		JsonObject formJson = env.getObject(requestFormParametersEnvKey);
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		for (String key : formJson.keySet()) {
			JsonElement json = formJson.get(key);
			if (json.isJsonObject()) {
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
			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
			try {
				ResponseEntity<String> response = restTemplate.exchange(endpointUri, HttpMethod.POST, request, String.class);
				addFullResponse(env, response);
			} catch (RestClientResponseException e) {
				return handleRestClientResponseException(env, e);
			} catch (RestClientException e) {
				return handleClientException(env, e);
			}

			logSuccess("Got " + endpointName + " response", env.getObject(responseEnvironmentKey));
			return env;
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException |
				 KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}
	}
}
