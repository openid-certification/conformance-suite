package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import org.apache.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.PreEnvironment;

public class DisallowAccessTokenInQuery extends AbstractCallProtectedResource {

	@Override
	@PreEnvironment(required = { "access_token", "resource" }, strings = "protected_resource_url")
	public Environment evaluate(Environment env) {

		return callProtectedResource(env);
	}

	@Override
	protected String getUri(Environment env) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(super.getUri(env));
		builder.queryParam("access_token", getAccessToken(env));

		return builder.toUriString();
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
