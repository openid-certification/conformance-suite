package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;
import org.springframework.http.MediaType;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;

public class CallKSASignedAccountRequestsEndpointWithBearerToken extends CallKSAAccountRequestsEndpointWithBearerToken {

	@Override
	protected Object getBody(Environment env) {
		String signed = env.getString("account_requests_endpoint_request_signed");
		if (Strings.isNullOrEmpty(signed)) {
			throw error("No signed account-requests consent JWT found; CreateKSAConsentRequest + SignKSAConsentRequest must run first");
		}
		return signed;
	}

	@Override
	protected MediaType getContentType(Environment env) {
		return MediaType.valueOf("application/jwt");
	}

	@Override
	protected List<MediaType> getAcceptContentType(Environment env) {
		return Collections.singletonList(MediaType.valueOf("application/jwt"));
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		if (Strings.isNullOrEmpty(responseBody)) {
			throw error("Empty/missing response from the account requests endpoint");
		}
		log("Account requests endpoint response", args("account_requests_endpoint_response_jwt", responseBody));

		JsonObject parsed;
		try {
			parsed = JWTUtil.jwtStringToJsonObjectForEnvironment(responseBody.trim());
		} catch (ParseException e) {
			throw error("KSA account-requests response is not a parseable JWT (the KSA spec requires application/jwt)", e,
				args("response", responseBody));
		}
		if (parsed == null || !parsed.has("claims")) {
			throw error("Could not parse KSA account-requests response JWT", args("response", responseBody));
		}

		JsonObject claims = parsed.getAsJsonObject("claims");
		// OBCreateConsentResponseSigned wraps the response under "message"; fall back to the claims themselves.
		JsonObject responseObject = claims.has("message") && claims.get("message").isJsonObject()
			? claims.getAsJsonObject("message")
			: claims;

		env.putObject("account_requests_endpoint_response", responseObject);
		env.putObject("resource_endpoint_response_full", responseObject);
		env.putObject("resource_endpoint_response_headers", responseHeaders);

		logSuccess("Parsed account requests endpoint response JWT",
			args("body", responseObject, "headers", responseHeaders));
		return env;
	}
}
