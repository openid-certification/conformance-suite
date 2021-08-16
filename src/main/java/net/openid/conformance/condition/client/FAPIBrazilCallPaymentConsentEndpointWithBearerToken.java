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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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


public class FAPIBrazilCallPaymentConsentEndpointWithBearerToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "access_token", "resource", "resource_endpoint_request_headers" }, strings = "consent_endpoint_request_signed")
	@PostEnvironment(required = { "resource_endpoint_response_headers", "consent_endpoint_response_full" })
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

		String resourceEndpoint = env.getString("config", "resource.consentUrl");
		if (Strings.isNullOrEmpty(resourceEndpoint)) {
			throw error("consent url missing from configuration");
		}

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		String requestObject = env.getString("consent_endpoint_request_signed");

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpHeaders headers = headersFromJson(requestHeaders);

			headers.setAccept(Collections.singletonList(DATAUTILS_MEDIATYPE_APPLICATION_JWT));
			headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
			headers.set("Authorization", "Bearer " + accessToken);

			HttpEntity<String> request = new HttpEntity<>(requestObject, headers);

			ResponseEntity<String> response = restTemplate.exchange(resourceEndpoint, HttpMethod.POST, request, String.class);

			String responseBody = response.getBody();

			if (Strings.isNullOrEmpty(responseBody)) {
				throw error("Empty/missing response from the consent endpoint");
			} else {
				// save full response
				JsonObject responseInfo = convertResponseForEnvironment("payment consent", response);
				env.putObject("consent_endpoint_response_full", responseInfo);

				// also save just headers, as at least CheckForFAPIInteractionIdInResourceResponse needs them
				JsonObject responseHeaders = mapToJsonObject(response.getHeaders(), true); // lowercase incoming headers
				env.putObject("resource_endpoint_response_headers", responseHeaders);

				logSuccess("Consent endpoint response", responseInfo);

				return env;
			}
		} catch (RestClientResponseException e) {
			throw error("Error from the consent endpoint", args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		} catch (RestClientException e) {
			String msg = "Call to consent endpoint " + resourceEndpoint + " failed";
			if (e.getCause() != null) {
				msg += " - " +e.getCause().getMessage();
			}
			throw error(msg, e);
		}

	}

}
