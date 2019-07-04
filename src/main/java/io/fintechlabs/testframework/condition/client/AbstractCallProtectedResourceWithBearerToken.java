package io.fintechlabs.testframework.condition.client;

import org.springframework.http.HttpHeaders;

import io.fintechlabs.testframework.testmodule.Environment;

public abstract class AbstractCallProtectedResourceWithBearerToken extends AbstractCallProtectedResource {

	@Override
	protected HttpHeaders getHeaders(Environment env) {

		HttpHeaders headers = super.getHeaders(env);

		headers.set("Authorization", String.join(" ", "Bearer", getAccessToken(env)));

		return headers;
	}
}
