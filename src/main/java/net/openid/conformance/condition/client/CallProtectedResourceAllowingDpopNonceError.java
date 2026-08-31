package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.http.DpopNonceResponseHeader;
import net.openid.conformance.util.http.WwwAuthenticateHeaderValueParser;
import org.springframework.http.MediaType;
import org.springframework.http.InvalidMediaTypeException;


/**
 * This is to call a generic resource server endpoint with an access token using DPoP.
 * It will extract the required DPoP nonce in resource_server_dpop_nonce when use_dpop_nonce error is returned
 *
 * Note that this returns success if the HTTP transaction returns a valid response
 * (i.e. no network error occurred) - regardless of the http status - callers will
 * generally need to explicitly verify the http status.
 */
public class CallProtectedResourceAllowingDpopNonceError extends CallProtectedResource {


	@Override
	@PreEnvironment(required = "access_token", strings = "protected_resource_url")
	@PostEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {
		env.removeNativeValue("resource_endpoint_dpop_nonce_error");
		return super.evaluate(env);
	}

	@Override
	protected MediaType getContentType(Environment env) {
		String configuredContentType = env.getString("resource", "resourceMediaType");

		if (!Strings.isNullOrEmpty(configuredContentType)) {
			try {
				return MediaType.parseMediaType(configuredContentType);
			}
			catch (InvalidMediaTypeException e) {
			}
		}

		return super.getContentType(env);
	}

	@Override
	protected Object getBody(Environment env) {
		JsonElement requestBody = env.getElementFromObject("resource", "resourceRequestBody");

		if (requestBody != null) {
			return requestBody.toString();
		}

		return super.getBody(env);
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {

		super.handleClientResponse(env, responseCode, responseBody, responseHeaders, fullResponse);

		// checked whatever the status code was, so the violation is attributed to the response that carried it
		DpopNonceResponseHeader nonceHeader = DpopNonceResponseHeader.from(responseHeaders);
		if (nonceHeader.violation() != null) {
			throw error(nonceHeader.violation(), args("headers", responseHeaders));
		}

		if(null != fullResponse) {
			int status = OIDFJSON.getInt(responseCode.get("code"));
			if (status >= 200 && status < 300 && nonceHeader.nonce() != null) {
				// RFC 9449 §9: resource servers supply nonces the same way authorization servers do,
				// including §8.2's rotation via a successful response, so a nonce arriving on a 2xx
				// replaces the stored one for the next resource request. Nothing went wrong with this
				// request, so resource_endpoint_dpop_nonce_error (the retry trigger) stays unset.
				env.putString("resource_server_dpop_nonce", nonceHeader.nonce());
			}
			if(status == 401) {
				if (WwwAuthenticateHeaderValueParser.hasUseDpopNonceChallenge(responseHeaders)) {
					if (nonceHeader.nonce() == null) {
						throw error("The resource server returned a 'use_dpop_nonce' error but supplied"
							+ " no DPoP-Nonce header, leaving no nonce to retry the request with.",
							args("headers", responseHeaders));
					}
					env.putString("resource_server_dpop_nonce", nonceHeader.nonce());
					env.putString("resource_endpoint_dpop_nonce_error", nonceHeader.nonce());
				}
			}
		}
		return env;
	}

}
