package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

public class CallTokenEndpointAndReturnFullResponse extends AbstractCallOAuthEndpoint {

	@Override
	@PreEnvironment(required = { "server", "token_endpoint_request_form_parameters" })
	@PostEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		return callTokenEndpoint(env, new DefaultResponseErrorHandler() {
			@Override
			public boolean hasError(ClientHttpResponse response) throws IOException {
				// Treat all http status codes as 'not an error', so spring never throws an exception due to the http
				// status code meaning the rest of our code can handle http status codes how it likes
				return false;
			}
		});
	}


	public Environment callTokenEndpoint(Environment env, ResponseErrorHandler errorHandler) {

		// build up the form
		final String requestFormParametersEnvKey = "token_endpoint_request_form_parameters";
		final String requestHeadersEnvKey = "token_endpoint_request_headers";
		final String tokenEndpointUri = env.getString("token_endpoint") != null ? env.getString("token_endpoint") : env.getString("server", "token_endpoint");
		final String endpointName = "token endpoint";
		final String envResponseKey = "token_endpoint_response_full";

		return callOAuthEndpoint(env, errorHandler, requestFormParametersEnvKey, requestHeadersEnvKey, tokenEndpointUri, endpointName, envResponseKey);

	}

	@Override
	protected void addFullResponse(Environment env, ResponseEntity<String> response) {
		super.addFullResponse(env, response);

		// add some legacy values - ideally we would refactor all the conditions that use these to read from
		// token_endpoint_response_full instead.
		env.putInteger("token_endpoint_response_http_status", response.getStatusCode().value());

		JsonObject responseHeaders = mapToJsonObject(response.getHeaders(), true);

		env.putObject("token_endpoint_response_headers", responseHeaders);

		JsonElement bodyJson = env.getElementFromObject("token_endpoint_response_full", "body_json");
		if (bodyJson != null) {
			env.putObject("token_endpoint_response", bodyJson.getAsJsonObject());
		}
	}
}
