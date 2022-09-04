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


public class FAPIBrazilCallPaymentConsentEndpointWithBearerToken extends CallProtectedResource {

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
		return env.getString("consent_endpoint_request_signed");
	}

	@Override
	protected MediaType getContentType(Environment env) {
		return DATAUTILS_MEDIATYPE_APPLICATION_JWT;
	}

	@Override
	protected HttpHeaders getHeaders(Environment env) {
		HttpHeaders headers = super.getHeaders(env);
		headers.setAccept(Collections.singletonList(DATAUTILS_MEDIATYPE_APPLICATION_JWT));
		return headers;
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		if (Strings.isNullOrEmpty(responseBody)) {
			throw error("Empty/missing response from the consent endpoint");
		}

		env.putObject("consent_endpoint_response_full", fullResponse);

		// also save just headers, as at least CheckForFAPIInteractionIdInResourceResponse needs them
		env.putObject("resource_endpoint_response_headers", responseHeaders);

		logSuccess("Payment consent endpoint response", fullResponse);
		return env;
	}

	@Override
	@PreEnvironment(required = { "access_token", "resource", "resource_endpoint_request_headers" }, strings = "consent_endpoint_request_signed")
	@PostEnvironment(required = { "resource_endpoint_response_headers", "consent_endpoint_response_full" })
	public Environment evaluate(Environment env) {
		return callProtectedResource(env);
	}

}
