package io.fintechlabs.testframework.condition.client;

import org.springframework.web.client.RestClientResponseException;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * This is to call a generic resource server endpoint with a BAD Bearer Token.
 * We should return the env if we catch a 401 back from the resource server, and throw
 * a {@link io.fintechlabs.testframework.condition.ConditionError} if it comes back ok.
 */
public class CallProtectedResourceWithInactiveBearerToken extends AbstractCallProtectedResource {

	@Override
	@PreEnvironment(required = { "access_token", "resource" }, strings = "protected_resource_url")
	@PostEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {

		return callProtectedResource(env);
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders) {

		throw error("Call to Resource Endpoint did not fail with bad token");
	}

	@Override
	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {

		if (e.getRawStatusCode() >=400 && e.getRawStatusCode() < 500) {
			env.putString("resource_endpoint_response", e.getStatusText());
			logSuccess("Resource endpoint correctly rejected access token");
			return env;
		} else {
			return super.handleClientResponseException(env, e);
		}
	}
}
