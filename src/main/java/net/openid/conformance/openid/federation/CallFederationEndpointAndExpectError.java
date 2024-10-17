package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCallProtectedResource;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

public class CallFederationEndpointAndExpectError extends AbstractCallProtectedResource {

	@Override
	@PreEnvironment(strings = { "federation_endpoint_url" })
	@PostEnvironment(required = "federation_response_body")
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
		return env.getString("federation_endpoint_url");
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		throw error("Got a successful response from the endpoint but an error was expected", args("body", responseBody));
	}

	@Override
	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {
		HttpHeaders responseHeaders = e.getResponseHeaders();
		String contentType = null;
		if (responseHeaders != null && responseHeaders.getContentType()!= null) {
			contentType = responseHeaders.getContentType().toString();
		}
		String body = e.getResponseBodyAsString();
		env.putString("endpoint_response", "headers.content-type", contentType);
		env.putString("endpoint_response_body_string", body);
		try {
			JsonObject responseBodyJson = JsonParser.parseString(body).getAsJsonObject();
			env.putObject("endpoint_response_body", responseBodyJson);
		} catch (JsonSyntaxException ignored) { }
		logSuccess("The request was rejected",
			args("code", e.getStatusCode().value(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
		return env;
	}

}
