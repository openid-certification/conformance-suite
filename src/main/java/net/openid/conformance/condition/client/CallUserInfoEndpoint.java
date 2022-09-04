package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;

public class CallUserInfoEndpoint extends AbstractCallProtectedResourceWithBearerToken {

	@Override
	protected boolean treatAllHttpStatusAsSuccess() {
		// Treat all http status codes as 'not an error', so spring never throws an exception due to the http
		// status code meaning the rest of our code can handle http status codes how it likes
		return true;
	}

	@Override
	protected HttpHeaders getHeaders(Environment env) {

		HttpHeaders headers = super.getHeaders(env);

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		headers = headersFromJson(requestHeaders, headers);

		return headers;
	}

	@Override
	@PreEnvironment(required = { "access_token", "server" })
	@PostEnvironment(required = {"userinfo_endpoint_response_full"})
	public Environment evaluate(Environment env) {
		return callProtectedResource(env);
	}

	@Override
	protected String getUri(Environment env) {

		String resourceUri = null;

		if (env.containsObject("mutual_tls_authentication")) {
			resourceUri = env.getString("server", "mtls_endpoint_aliases.userinfo_endpoint");
		}
		if (resourceUri == null) {
			resourceUri = env.getString("server", "userinfo_endpoint");
		}
		if (Strings.isNullOrEmpty(resourceUri)){
			throw error("\"userinfo_endpoint\" missing from server configuration");
		}

		return resourceUri;
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {

		env.putObject("userinfo_endpoint_response_full", fullResponse);

		logSuccess("Got a response from the userinfo endpoint", args("body", responseBody, "headers", responseHeaders, "status_code", responseCode));
		return env;
	}
}
