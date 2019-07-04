package io.fintechlabs.testframework.condition.client;

import org.springframework.http.HttpHeaders;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CallProtectedResourceWithBearerTokenAndCustomHeaders extends CallProtectedResourceWithBearerToken {

	@Override
	@PreEnvironment(required = { "access_token", "resource" }, strings = "protected_resource_url")
	@PostEnvironment(required = "resource_endpoint_response_headers", strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {

		return callProtectedResource(env);
	}

	@Override
	protected HttpHeaders getHeaders(Environment env) {

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");
		HttpHeaders headers = headersFromJson(requestHeaders);

		headers.set("Authorization", String.join(" ", "Bearer", getAccessToken(env)));

		return headers;
	}
}
