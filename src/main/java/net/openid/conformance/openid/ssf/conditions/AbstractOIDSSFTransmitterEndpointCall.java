package net.openid.conformance.openid.ssf.conditions;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCallProtectedResourceWithBearerToken;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientResponseException;

public abstract class AbstractOIDSSFTransmitterEndpointCall extends AbstractCallProtectedResourceWithBearerToken {

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
		return false;
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
		String resourceUrl = getResourceEndpointUrl(env);
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
		logSuccess("Called resource endpoint");

		return env;
	}
}
