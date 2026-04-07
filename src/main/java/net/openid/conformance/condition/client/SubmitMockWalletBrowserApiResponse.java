package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

/**
 * Integration-test-only condition: wraps the prepared VP response in DC API format
 * and POSTs it to the wallet test's own browser API submit URL, simulating a real
 * wallet's DC API response.
 *
 * Expects the caller to have already generated the credential, built the VP token,
 * and optionally encrypted the response using the standard conditions
 * (CreateSdJwtKbCredential, AddVP1FinalDCQLVPTokenToAuthorizationEndpointResponseParams,
 * VP1FinalEncryptVPResponse).
 *
 * Uses createRestTemplate() so the lock is released during the HTTP call, allowing
 * handleBrowserApiSubmission to process the response without deadlocking.
 */
public class SubmitMockWalletBrowserApiResponse extends AbstractCallEndpoint {

	@Override
	@PreEnvironment(required = {"browser_api_request", "browser_api_submit"})
	public Environment evaluate(Environment env) {

		String submitUrl = env.getString("browser_api_submit", "fullUrl");

		JsonArray requests = env.getElementFromObject("browser_api_request", "digital.requests").getAsJsonArray();
		String protocol = OIDFJSON.getString(requests.get(0).getAsJsonObject().get("protocol"));

		JsonObject dcApiResponse = new JsonObject();
		dcApiResponse.addProperty("protocol", protocol);

		// If VP1FinalEncryptVPResponse ran, the encrypted response is in direct_post_request_form_parameters.
		// Otherwise use the unencrypted authorization_endpoint_response_params.
		JsonObject formParams = env.getObject("direct_post_request_form_parameters");
		if (formParams != null && formParams.has("response")) {
			// Encrypted: wrap the JWE in a "response" field
			dcApiResponse.add("data", formParams);
		} else {
			// Unencrypted: use response params directly
			dcApiResponse.add("data", env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY));
		}

		postToSubmitUrl(env, submitUrl, dcApiResponse.toString());

		logSuccess("Submitted mock wallet response to browser API submit URL",
			args("submit_url", submitUrl, "protocol", protocol,
				"encrypted", formParams != null && formParams.has("response")));

		return env;
	}

	private void postToSubmitUrl(Environment env, String submitUrl, String jsonBody) {
		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
			ResponseEntity<String> response = restTemplate.exchange(submitUrl, HttpMethod.POST, request, String.class);

			int statusCode = response.getStatusCode().value();
			if (statusCode != 204 && statusCode != 200) {
				throw error("Unexpected response from submit URL",
					args("response_code", statusCode, "submit_url", submitUrl,
						"response_body", response.getBody()));
			}
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException |
				 InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Failed to create HTTP client for mock POST", e);
		}
	}

}
