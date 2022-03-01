package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;

/**
 * This is to call a generic resource server endpoint with an access token.
 *
 * Note that this returns success if the HTTP transaction returns a valid response
 * (i.e. no network error occurred) - regardless of the http status - callers will
 * generally need to explicitly verify the http status.
 */
public class CallProtectedResource extends AbstractCallProtectedResourceWithBearerToken {

	@Override
	protected boolean treatAllHttpStatusAsSuccess() {
		// Treat all http status codes as 'not an error', so spring never throws an exception due to the http
		// status code meaning the rest of our code can handle http status codes how it likes
		return true;
	}

	@Override
	@PreEnvironment(required = "access_token", strings = "protected_resource_url")
	@PostEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {

		return callProtectedResource(env);
	}

	@Override
	protected HttpHeaders getHeaders(Environment env) {

		HttpHeaders headers = super.getHeaders(env);

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		headers = headersFromJson(requestHeaders, headers);

		return headers;
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {

		env.putObject("resource_endpoint_response_full", fullResponse);

		// Temporarily store to "old" environment locations; these are deprecated and we
		// should change conditions to use resource_endpoint_response_full to avoid
		// having the same information stored in different places.
		env.putString("resource_endpoint_response", responseBody);
		env.putObject("resource_endpoint_response_headers", responseHeaders);

		// Once we've done the above, we should make this condition explicitly remove
		// the old locations, as other conditions may still be writing to them and we
		// don't want to accidentally use data from other responses:
//		env.removeNativeValue("resource_endpoint_response");
//		env.removeObject("resource_endpoint_response_headers");

		logSuccess("Got a response from the resource endpoint", fullResponse);
		return env;
	}

}
