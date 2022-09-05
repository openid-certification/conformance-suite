package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Collections;


public class CallConsentEndpointWithBearerToken extends CallProtectedResource {

	@Override
	protected String getUri(Environment env) {
		String consentUrl = env.getString("config", "resource.consentUrl");
		if (Strings.isNullOrEmpty(consentUrl)) {
			throw error("consent url missing from configuration");
		}
		return consentUrl;
	}

	@Override
	protected HttpMethod getMethod(Environment env) {
		return HttpMethod.POST;
	}

	@Override
	protected Object getBody(Environment env) {
		return env.getObject("consent_endpoint_request").toString();
	}

	@Override
	protected MediaType getContentType(Environment env) {
		return MediaType.APPLICATION_JSON;
	}

	@Override
	protected HttpHeaders getHeaders(Environment env) {
		HttpHeaders headers = super.getHeaders(env);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		return headers;
	}

	@Override
	protected boolean requireJsonResponseBody() {
		return true;
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {

		env.putObject("consent_endpoint_response_full", fullResponse);

		// Temporarily store to "old" environment locations; these are deprecated and we
		// should change conditions to use resource_endpoint_response_full to avoid
		// having the same information stored in different places.
		env.putObject("consent_endpoint_response", fullResponse.get("body_json").getAsJsonObject());
		env.putObject("resource_endpoint_response_headers", responseHeaders);

		// Once we've done the above, we should make this condition explicitly remove
		// the old locations, as other conditions may still be writing to them and we
		// don't want to accidentally use data from other responses:
//		env.removeNativeValue("consent_endpoint_response");
//		env.removeObject("resource_endpoint_response_headers");

		logSuccess("Got a response from the consent endpoint", fullResponse);
		return env;
	}

	@Override
	@PreEnvironment(required = { "access_token", "resource", "consent_endpoint_request", "resource_endpoint_request_headers" })
	@PostEnvironment(required = { "resource_endpoint_response_headers", "consent_endpoint_response", "consent_endpoint_response_full" })
	public Environment evaluate(Environment env) {
		return callProtectedResource(env);
	}

}
