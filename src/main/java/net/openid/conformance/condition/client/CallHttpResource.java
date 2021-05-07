package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.*;
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


public class CallHttpResource extends AbstractCondition {

	private JsonParser jsonParser = new JsonParser();

	@Override
	@PreEnvironment(strings = "resource_url")
	@PostEnvironment(required = "resource_endpoint_response")
	public Environment evaluate(Environment env) {
		String uri = getUri(env);

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpMethod method = getMethod(env);
			HttpHeaders headers = getHeaders(env);

			if (headers.getAccept().isEmpty()) {
				headers.setAccept(Collections.singletonList(DATAUTILS_MEDIATYPE_APPLICATION_JSON_UTF8));
				headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
			}

			if (method == HttpMethod.POST) {
				headers.setContentType(getMediaType(env));
			}

			HttpEntity<?> request = new HttpEntity<>(getBody(env), headers);

			ResponseEntity<String> response = restTemplate.exchange(uri, method, request, String.class);
			JsonObject responseCode = new JsonObject();
			responseCode.addProperty("code", response.getStatusCodeValue());
			String responseBody = response.getBody();
			JsonObject responseHeaders = mapToJsonObject(response.getHeaders(), true);

			return handleClientResponse(env, responseCode, responseBody, responseHeaders);
		} catch (RestClientResponseException e) {
			return handleClientResponseException(env, e);
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (RestClientException e) {
			String msg = "Call to protected resource " + uri + " failed";
			if (e.getCause() != null) {
				msg += " - " +e.getCause().getMessage();
			}
			throw error(msg, e);
		}
	}

	protected String getUri(Environment env) {

		String resourceUri = env.getString( "resource_url");

		if (Strings.isNullOrEmpty(resourceUri)){
			throw error("Missing Resource URL");
		}

		return resourceUri;
	}

	protected HttpMethod getMethod(Environment env) {

		HttpMethod resourceMethod = HttpMethod.GET;
		String configuredMethod = env.getString("resource_method");
		if (!Strings.isNullOrEmpty(configuredMethod)) {
			resourceMethod = HttpMethod.valueOf(configuredMethod);
		}

		return resourceMethod;
	}

	protected HttpHeaders getHeaders(Environment env) {
		if(!env.containsObject("request_headers")) {
			return new HttpHeaders();
		}
		JsonObject headersJson = env.getObject("request_headers");
		return this.headersFromJson(headersJson);
	}

	protected MediaType getMediaType(Environment env) {

		return MediaType.APPLICATION_JSON;
	}

	protected String getBody(Environment env) {

		return env.getString("request_body");
	}

	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders) {

		JsonObject jsonBody = responseBody == null ? null : OIDFJSON.toObject(jsonParser.parse(responseBody));
		env.putObject("resource_endpoint_response_code", responseCode);
		env.putObject("resource_endpoint_response", jsonBody);
		env.putObject("resource_endpoint_response_headers", responseHeaders);

		logSuccess("Got a response from the resource endpoint", args("body", responseBody, "headers", responseHeaders, "status_code", responseCode));
		return env;
	}

	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {
		throw error("Unexpected error from the resource endpoint", e, args("code", e.getRawStatusCode(), "status", e.getStatusText()));
	}

}
