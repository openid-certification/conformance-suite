package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateTokenEndpointWithExpiresInResponse extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "access_token", "token_type" }, required = "backchannel_request_object") // note the others are optional
	@PostEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		String accessToken = env.getString("access_token");
		String tokenType = env.getString("token_type");
		String idToken = env.getString("id_token");
		String refreshToken = env.getString("refresh_token");
		String scope = env.getString("scope");

		if (Strings.isNullOrEmpty(accessToken) || Strings.isNullOrEmpty(tokenType)) {
			throw error("Missing required access_token or token_type");
		}

		JsonObject tokenEndpointResponse = new JsonObject();

		tokenEndpointResponse.addProperty("access_token", accessToken);
		tokenEndpointResponse.addProperty("token_type", tokenType);

		if (!Strings.isNullOrEmpty(idToken)) {
			tokenEndpointResponse.addProperty("id_token", idToken);
		}

		if (!Strings.isNullOrEmpty(refreshToken)) {
			tokenEndpointResponse.addProperty("refresh_token", refreshToken);
		}

		if (!Strings.isNullOrEmpty(scope)) {
			tokenEndpointResponse.addProperty("scope", scope);
		}

		// TODO: Repeated code, plus seems like a poor idea to pack that in here as well, I should probably use conditions
		// Also expires_in is optional, there should be tests without it as well ofc.
		String requestedExpiryString = env.getString("backchannel_request_object", "claims.requested_expiry");
		if(requestedExpiryString != null) {
			try {
				Integer requestedExpiryValue = Integer.parseInt(requestedExpiryString);
				if(requestedExpiryValue <= 0) {
					throw error("The 'requested_expiry' must be a positive integer when present.");
				}
				tokenEndpointResponse.addProperty("expires_in", requestedExpiryValue);
			} catch (NumberFormatException nfe) {
				throw error("The 'requested_expiry' must be a positive integer when present.");
			}
		} else{
			tokenEndpointResponse.addProperty("expires_in", 180);
		}

		env.putObject("token_endpoint_response", tokenEndpointResponse);

		logSuccess("Created token endpoint response", tokenEndpointResponse);

		return env;

	}

}
