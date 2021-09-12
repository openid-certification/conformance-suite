package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

public class CallProtectedResourceWithBearerTokenAndCustomHeadersOptionalError extends CallProtectedResourceWithBearerToken {

	@Override
	@PreEnvironment(required = { "access_token", "resource", "resource_endpoint_request_headers" }, strings = "protected_resource_url")
	@PostEnvironment(required = "resource_endpoint_response_headers", strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {

		return callProtectedResource(env);
	}

	@Override
	protected HttpHeaders getHeaders(Environment env) {

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");
		HttpHeaders headers = headersFromJson(requestHeaders);

		headers.set("Authorization", "Bearer " + getAccessToken(env));

		return headers;
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {

		env.putString("resource_endpoint_response", responseBody);
		env.putObject("resource_endpoint_response_headers", responseHeaders);
		env.putObject("resource_endpoint_response_full", fullResponse);
		env.putInteger("resource_endpoint_response_status", OIDFJSON.getInt(responseCode.get("code")));

		logSuccess("Got a response from the resource endpoint", fullResponse);
		return env;

	}

	@Override
	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {

		logSuccess("Resource endpoint returned error", args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));

		env.putInteger("resource_endpoint_response_status", e.getRawStatusCode());
		env.putString("resource_endpoint_error_code", String.valueOf(e.getRawStatusCode()));

		return env;
	}
}
