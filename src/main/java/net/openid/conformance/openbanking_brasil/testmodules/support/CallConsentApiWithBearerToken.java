package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking.FAPIOBGetResourceEndpoint;
import net.openid.conformance.testmodule.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Optional;


public class CallConsentApiWithBearerToken extends AbstractCondition {

	private static final Logger logger = LoggerFactory.getLogger(net.openid.conformance.condition.client.CallConsentEndpointWithBearerToken.class);

	@Override
	@PreEnvironment(required = { "access_token",
		                         "resource",
		                         "consent_endpoint_request",
								 "resource_endpoint_request_headers" },
					strings = "http_method")
	@PostEnvironment(required = { "resource_endpoint_response_headers"}, strings = { "resource_endpoint_response" })
	public Environment evaluate(Environment env) {

		String accessToken = env.getString("access_token", "value");
		if (Strings.isNullOrEmpty(accessToken)) {
			throw error("Access token not found");
		}

		String method = env.getString("http_method");
		if (Strings.isNullOrEmpty(method)) {
			throw error("HTTP method not found");
		}

		log("Preparing to call endpoint with HTTP method " + method);

		String tokenType = env.getString("access_token", "type");
		if (Strings.isNullOrEmpty(tokenType)) {
			throw error("Token type not found");
		} else if (!tokenType.equalsIgnoreCase("Bearer")) {
			throw error("Access token is not a bearer token", args("token_type", tokenType));
		}

		String resourceEndpoint = Optional.ofNullable(env.getString("consent_url"))
		.orElseGet(() -> env.getString("config", "resource.consentUrl"));

		if (Strings.isNullOrEmpty(resourceEndpoint)) {
			throw error("consent url missing from configuration");
		}

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		JsonObject requestObject = env.getObject("consent_endpoint_request");

		Boolean expectBody = Optional.ofNullable(env.getBoolean("expect_response_body")).orElse(true);
		Boolean ignoreResponseErrors = Optional.ofNullable(env.getBoolean("ignore_response_errors")).orElse(false);


		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpHeaders headers = headersFromJson(requestHeaders);

			headers.setAccept(Collections.singletonList(DATAUTILS_MEDIATYPE_APPLICATION_JSON_UTF8));
			headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
			headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JSON_UTF8);
			headers.set("Authorization", "Bearer " + accessToken);


			// Stop RestTemplate from overwriting the Accept-Charset header
			StringHttpMessageConverter converter = new StringHttpMessageConverter();
			converter.setWriteAcceptCharset(false);
			restTemplate.setMessageConverters(Collections.singletonList(converter));

			HttpEntity<String> request = new HttpEntity<>(requestObject.toString(), headers);

			HttpMethod httpMethod = HttpMethod.resolve(method);

			ResponseEntity<String> response = restTemplate.exchange(resourceEndpoint, httpMethod, request, String.class);

			String jsonString = response.getBody();

			if(!expectBody) {
				if (!Strings.isNullOrEmpty(jsonString)) {
					throw error("Was not expecting a response entity here");
				} else {
					JsonObject responseHeaders = mapToJsonObject(response.getHeaders(), true); // lowercase incoming headers
					env.putObject("resource_endpoint_response_headers", responseHeaders);

					logSuccess("No response body, as expected", args( "headers", responseHeaders));
					return env;
				}
			}

			if (Strings.isNullOrEmpty(jsonString)) {
				throw error("Empty/missing response from the consent endpoint");
			} else {
				log("Consent endpoint response", args("resource_endpoint_response", jsonString));

				try {
					JsonElement jsonRoot = new JsonParser().parse(jsonString);
					if (jsonRoot == null || !jsonRoot.isJsonObject()) {
						throw error("Consent endpoint did not return a JSON object");
					}

					JsonObject responseHeaders = mapToJsonObject(response.getHeaders(), true); // lowercase incoming headers

					env.putString("resource_endpoint_response", jsonString);
					env.putObject("resource_endpoint_response_headers", responseHeaders);
					env.putInteger("resource_endpoint_response_status", response.getStatusCodeValue());

					logSuccess("Parsed consent endpoint response", args("body", jsonString, "headers", responseHeaders));

					return env;
				} catch (JsonParseException e) {
					throw error(e);
				}
			}
		} catch (RestClientResponseException e) {
			if(!ignoreResponseErrors) {
				env.putInteger("resource_endpoint_response_status", e.getRawStatusCode());
				logger.warn("Exception: ", e);
				throw error("Error from the consent endpoint", e, args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			} else {
				JsonObject responseDetails = new JsonObject();
				responseDetails.addProperty("status_code", e.getRawStatusCode());
				responseDetails.addProperty("status_message", e.getStatusText());
				responseDetails.add("response_headers", mapToJsonObject(e.getResponseHeaders(), false));
				env.putObject("errored_response", responseDetails);
				return env;
			}
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			logger.warn("Error creating HTTP Client", e);
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
