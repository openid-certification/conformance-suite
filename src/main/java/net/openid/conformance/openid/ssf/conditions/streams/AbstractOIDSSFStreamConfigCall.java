package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCallProtectedResourceWithBearerToken;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientResponseException;

public abstract class AbstractOIDSSFStreamConfigCall extends AbstractCallProtectedResourceWithBearerToken {

	@Override
	protected MediaType getContentType(Environment env) {
		return MediaType.APPLICATION_JSON;
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		env.putObject("resource_endpoint_response_full", fullResponse);
		return env;
	}

	protected boolean throwOnClientResponseException() {
		return true;
	}

	@Override
	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {
		if (throwOnClientResponseException()) {
			return super.handleClientResponseException(env, e);
		}

		log("Received error from the resource endpoint", args("code", e.getStatusCode().value(), "status", e.getStatusText()));

		JsonObject errorEndpointResponse = new JsonObject();
		errorEndpointResponse.addProperty("status", e.getStatusCode().value());
		errorEndpointResponse.addProperty("endpoint_name", getEndpointName());

		env.putObject("resource_endpoint_response_full", errorEndpointResponse);

		return env;
	}

	protected abstract String getEndpointName();

	protected void configureResourceUrl(Environment env) {
		String configurationEndpoint = getConfigurationEndpoint(env);
		env.putString("protected_resource_url", configurationEndpoint);
	}

	protected String getConfigurationEndpoint(Environment env) {
		return env.getString("transmitter_metadata", "configuration_endpoint");
	}

	protected String getStatusEndpoint(Environment env) {
		return env.getString("transmitter_metadata", "status_endpoint");
	}

	protected void configureAccessToken(Environment env) {
		String transmitterAccessToken = getTransmitterAccessToken(env);
		env.putString("access_token", "value", transmitterAccessToken);
		env.putString("access_token", "type", "Bearer");
	}

	protected String getTransmitterAccessToken(Environment env) {
		return env.getString("transmitter_access_token");
	}

	@Override
	protected boolean requireJsonResponseBody() {
		return true;
	}

	protected void prepareRequest(Environment env) {

	}

	protected String getStreamStatusEndpointUrlWithStreamId(Environment env) {
		return appendStreamIdIfPresent(getStatusEndpoint(env), env);
	}

	protected String getStreamConfigEndpointUrlWithStreamIdIfPresent(Environment env) {
		return appendStreamIdIfPresent(getConfigurationEndpoint(env), env);
	}

	private String appendStreamIdIfPresent(String endpoint, Environment env) {
		String streamId = getStreamId(env);
		String effectiveEndpoint = endpoint;
		if (streamId != null) {
			effectiveEndpoint = effectiveEndpoint + "?stream_id=" + streamId;
		}
		return effectiveEndpoint;
	}

	protected String getStreamId(Environment env) {
		return env.getString("stream_id");
	}

	@PreEnvironment(required = {"config", "transmitter_metadata"})
	@Override
	public Environment evaluate(Environment env) {

		configureAccessToken(env);
		configureResourceUrl(env);
		prepareRequest(env);

		log("Calling resource endpoint");
		callProtectedResource(env);

		return env;
	}
}
