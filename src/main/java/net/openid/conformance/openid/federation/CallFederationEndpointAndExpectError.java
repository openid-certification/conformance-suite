package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractCallProtectedResource;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

public class CallFederationEndpointAndExpectError extends AbstractCallProtectedResource {

	@Override
	public Environment evaluate(Environment env) {
		return callProtectedResource(env);
	}

	@Override
	protected HttpHeaders getHeaders(Environment env) {
		HttpHeaders headers = new HttpHeaders();
		headers.put("accept", List.of("*/*"));
		return headers;
	}

	@Override
	protected String getUri(Environment env) {
		return env.getString("entity_statement_url");
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		throw error("Got a successful response from the endpoint but an error was expected", args("body", responseBody));
	}

	@Override
	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {
		logSuccess("The request was rejected",
			args("code", e.getStatusCode().value(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
		return env;
	}

}
