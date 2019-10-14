package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;

import com.google.gson.JsonObject;

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
