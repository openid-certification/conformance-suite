package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * This is to call a generic resource server endpoint with a Bearer Token.
 */
public class CallProtectedResourceWithBearerToken extends AbstractCallProtectedResourceWithBearerToken {

	@Override
	@PreEnvironment(required = "access_token", strings = "protected_resource_url")
	@PostEnvironment(required = "resource_endpoint_response_headers", strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {

		return callProtectedResource(env);
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {

		env.putObject("resource_endpoint_response_code", responseCode);
		env.putString("resource_endpoint_response", responseBody);
		env.putObject("resource_endpoint_response_headers", responseHeaders);
		env.putObject("resource_endpoint_response_full", fullResponse);

		logSuccess("Got a response from the resource endpoint", fullResponse);
		return env;
	}
}
