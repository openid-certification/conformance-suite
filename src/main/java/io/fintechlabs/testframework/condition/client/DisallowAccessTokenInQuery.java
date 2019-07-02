package io.fintechlabs.testframework.condition.client;

import org.apache.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class DisallowAccessTokenInQuery extends AbstractCallProtectedResource {

	@Override
	@PreEnvironment(required = { "access_token", "resource" }, strings = "protected_resource_url")
	public Environment evaluate(Environment env) {

		return callProtectedResource(env);
	}

	@Override
	protected Environment callProtectedResource(Environment env, String resourceUri, HttpMethod resourceMethod, String accessToken) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(resourceUri);
		builder.queryParam("access_token", accessToken);

		return super.callProtectedResource(env, builder.toUriString(), resourceMethod, accessToken);
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders) {

		throw error("Got a successful response from the resource endpoint", args("body", responseBody));
	}

	@Override
	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {

		if (e.getRawStatusCode() == HttpStatus.SC_BAD_REQUEST ||
			e.getRawStatusCode() == HttpStatus.SC_UNAUTHORIZED ||
			e.getRawStatusCode() == HttpStatus.SC_REQUEST_URI_TOO_LONG) {
			logSuccess("Resource server refused request", args("code", e.getRawStatusCode(), "status", e.getStatusText()));
			return env;
		} else {
			return super.handleClientResponseException(env, e);
		}
	}
}
