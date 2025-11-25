package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

/**
 * This is to call a generic resource server endpoint with an access token.
 *
 * The autorization token type is forced to "Bearer"
 */
public class CallProtectedResourceForceBearer extends CallProtectedResource {

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
	protected HttpHeaders getHeaders(Environment env) {

		HttpHeaders headers = super.getHeaders(env);

		String accessToken = env.getString("access_token", "value");
		if (Strings.isNullOrEmpty(accessToken)) {
			throw error("Access token not found");
		}

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		headers = headersFromJson(requestHeaders, headers);

		headers.set("Authorization", "Bearer " + accessToken);

		return headers;
	}
}
