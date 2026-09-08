package net.openid.conformance.openid.ssf.conditions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCallProtectedResourceWithBearerToken;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public abstract class AbstractOIDSSFTransmitterEndpointCall extends AbstractCallProtectedResourceWithBearerToken {

	@Override
	protected MediaType getContentType(Environment env) {
		return MediaType.APPLICATION_JSON;
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		env.putObject("resource_endpoint_response_full", fullResponse);
		env.putString("endpoint_response_body_string", responseBody);
		return env;
	}

	protected boolean throwOnClientResponseException() {
		return false;
	}

	@Override
	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {

		JsonObject errorEndpointResponse = new JsonObject();
		errorEndpointResponse.addProperty("status", e.getStatusCode().value());
		errorEndpointResponse.addProperty("endpoint_name", getEndpointName());
		errorEndpointResponse.addProperty("body", e.getResponseBodyAsString());
		if (e.getResponseHeaders() != null) {
			// keep the response headers available for follow-up checks (e.g. the RFC 6750
			// WWW-Authenticate challenge on 401/403 responses)
			errorEndpointResponse.add("headers", mapToJsonObject(e.getResponseHeaders(), true));
		}
		MediaType responseContentType = e.getResponseHeaders().getContentType();
		if (responseContentType != null && (MediaType.APPLICATION_JSON.equals(responseContentType) ||
			// deal with funky vendor specific content types like application/vnd.foo.bar+json
			(MediaType.APPLICATION_JSON.getType().equals(responseContentType.getType())
				&& responseContentType.getSubtype().endsWith(MediaType.APPLICATION_JSON.getSubtype())))
		) {
			JsonElement bodyJson = JsonParser.parseString(e.getResponseBodyAsString());
			errorEndpointResponse.add("body_json", bodyJson);
		}

		env.putObject("resource_endpoint_response_full", errorEndpointResponse);


		if (throwOnClientResponseException()) {
			return super.handleClientResponseException(env, e);
		}

		log("Received error from the resource endpoint", args("code", e.getStatusCode().value(), "status", e.getStatusText()));

		return env;
	}

	protected abstract String getEndpointName();

	@Override
	protected HttpHeaders getHeaders(Environment env) {
		if (env.getString("ssf", "omit_authorization_header") != null) {
			// CAEPIOP 2.7.2 negative tests probe the transmitter's behavior for requests
			// that carry no bearer credentials in the Authorization header
			return new HttpHeaders();
		}
		return super.getHeaders(env);
	}

	protected void configureResourceUrl(Environment env) {
		String resourceUrl = getResourceEndpointUrl(env);
		// CAEPIOP 2.7.2 negative tests move the token into the URI query (RFC 6750 2.3)
		String queryToken = env.getString("ssf", "access_token_query_override");
		if (queryToken != null) {
			String encodedToken = URLEncoder.encode(queryToken, StandardCharsets.UTF_8);
			resourceUrl = resourceUrl + (resourceUrl.contains("?") ? "&" : "?") + "access_token=" + encodedToken;
		}
		env.putString("protected_resource_url", resourceUrl);
	}

	protected abstract String getResourceEndpointUrl(Environment env);

	protected String getConfigurationEndpointUrl(Environment env) {
		return env.getString("ssf", "transmitter_metadata.configuration_endpoint");
	}

	protected String getStatusEndpointUrl(Environment env) {
		return env.getString("ssf", "transmitter_metadata.status_endpoint");
	}

	protected String getAddSubjectEndpointUrl(Environment env) {
		return env.getString("ssf", "transmitter_metadata.add_subject_endpoint");
	}

	protected String getRemoveSubjectEndpointUrl(Environment env) {
		return env.getString("ssf", "transmitter_metadata.remove_subject_endpoint");
	}

	protected String getVerificationEndpointUrl(Environment env) {
		return env.getString("ssf", "transmitter_metadata.verification_endpoint");
	}

	protected void configureAccessToken(Environment env) {

	}

	@Override
	protected boolean requireJsonResponseBody() {
		return true;
	}

	protected void prepareRequest(Environment env) {
	}

	@PreEnvironment(required = {"config", "ssf"})
	@Override
	public Environment evaluate(Environment env) {

		configureAccessToken(env);
		configureResourceUrl(env);
		prepareRequest(env);

		callProtectedResource(env);
		log("Called resource endpoint",args("endpoint", env.getString("protected_resource_url")));

		return env;
	}
}
