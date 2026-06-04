package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
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

public class CallAuthzenApiEndpoint extends AbstractCondition {

	protected boolean allowJsonParseFailure() {
		return false;
	}

	@Override
	@PreEnvironment(required = {"authzen_api_endpoint_request"}, strings = "authzen_api_endpoint")
	@PostEnvironment(required = "authzen_api_endpoint_response")
	public Environment evaluate(Environment env) {

		String authzenApiEndpoint = env.getString("authzen_api_endpoint");
		if (authzenApiEndpoint == null) {
			throw error("Couldn't find Authzen API endpoint");
		}

		JsonObject requestObj = env.getObject("authzen_api_endpoint_request");

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

			HttpHeaders headers = new HttpHeaders();

			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
			String contentTypeOverride = env.getString("authzen_api_endpoint_request_content_type");
			if (contentTypeOverride != null) {
				if (contentTypeOverride.isEmpty()) {
					// Empty override means "send no Content-Type header at all". HttpHeaders.setContentType
					// won't accept null, so build the entity without ever setting it.
				} else {
					headers.setContentType(MediaType.parseMediaType(contentTypeOverride));
				}
			} else {
				headers.setContentType(MediaType.APPLICATION_JSON);
			}

			/*
			 * Add/Override headers for request
			 */
			JsonObject requestHeaders = env.getObject("authzen_api_endpoint_request_headers");
			if(null != requestHeaders && requestHeaders.isJsonObject()) {
				for(String header : requestHeaders.keySet()) {
					headers.set(header, OIDFJSON.getString(requestHeaders.get(header)));
				}
			}

			// Body: raw override takes precedence over the JSON object representation.
			String rawBody = env.getString("authzen_api_endpoint_request_raw_body");
			Object bodyToSend = rawBody != null ? rawBody : (requestObj != null ? requestObj.toString() : "");

			HttpEntity<?> request = new HttpEntity<>(bodyToSend, headers);

			HttpMethod method = HttpMethod.POST;
			String methodOverride = env.getString("authzen_api_endpoint_request_method");
			if (methodOverride != null) {
				method = HttpMethod.valueOf(methodOverride);
			}

			try {
				ResponseEntity<String> response = restTemplate.exchange(authzenApiEndpoint, method, request, String.class);

				JsonObject responseInfo = convertJsonResponseForEnvironment("Authzen API endpoint", response, allowJsonParseFailure());

				env.putObject("authzen_api_endpoint_response", responseInfo);

				log("Parsed Authzen API endpoint response", responseInfo);

				return env;


			} catch (RestClientResponseException e) {
				throw error("RestClientResponseException occurred whilst calling Authzen API endpoint",
					args("code", e.getStatusCode().value(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			} catch (RestClientException e) {
				return handleClientException(env, e);
			}

		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}
	}

	Environment handleClientException(Environment env, RestClientException e) {
		String msg = "Call to Authzen API endpoint failed";
		if (e.getCause() != null) {
			msg += " - " +e.getCause().getMessage();
		}
		throw error(msg, e);
	}
}
