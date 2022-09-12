package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class GenerateRefreshTokenRequest extends AbstractCondition {
	@Override
	@PreEnvironment(required = {"token_endpoint_response"})
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {
		JsonObject refreshTokenRequest = new JsonObject();
		refreshTokenRequest.addProperty("grant_type", "refresh_token");
		JsonObject tokenEndpointResponse = env.getObject("token_endpoint_response");
		String refreshToken = extractRefreshToken(env, tokenEndpointResponse);
		refreshTokenRequest.addProperty("refresh_token", refreshToken);

		env.putObject("token_endpoint_request_form_parameters", refreshTokenRequest);

		// Remove headers as well, so that we're truly starting a 'new' request
		env.removeObject("token_endpoint_request_headers");

		logSuccess("Created token endpoint request parameters", refreshTokenRequest);
		return env;
	}

	private String extractRefreshToken(Environment env, JsonObject tokenEndpointResponse) {

		if (tokenEndpointResponse.has("refresh_token")) {
			//Always check first the token response from a CallTokenEndpoint condition
			return OIDFJSON.getString(tokenEndpointResponse.get("refresh_token"));
		}

		String refreshToken = env.getString("refresh_token");
		if (!Strings.isNullOrEmpty(refreshToken)) {
			return refreshToken;
		}

		throw error("No refresh token found, verify the authorization server responses");
	}
}
