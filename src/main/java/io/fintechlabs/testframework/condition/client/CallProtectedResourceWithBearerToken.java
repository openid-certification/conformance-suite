package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * This is to call a generic resource server endpoint with a Bearer Token.
 */
public class CallProtectedResourceWithBearerToken extends AbstractCallProtectedResourceWithBearerToken {

	@Override
	@PreEnvironment(required = { "access_token", "resource" }, strings = "protected_resource_url")
	@PostEnvironment(required = "resource_endpoint_response_headers", strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {

		return callProtectedResource(env);
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders) {

		env.putObject("resource_endpoint_response_code", responseCode);
		env.putString("resource_endpoint_response", responseBody);
		env.putObject("resource_endpoint_response_headers", responseHeaders);

		logSuccess("Got a response from the resource endpoint", args("body", responseBody, "headers", responseHeaders, "status_code", responseCode));
		return env;
	}
}
