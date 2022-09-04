package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateTokenEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "access_token", "token_type" }) // note the others are optional
	@PostEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		String accessToken = env.getString("access_token");
		String tokenType = env.getString("token_type");
		String idToken = env.getString("id_token");
		String refreshToken = env.getString("refresh_token");
		String scope = env.getString("scope");
		String accessTokenExpiration = env.getString("access_token_expiration");

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

		if (!Strings.isNullOrEmpty(accessTokenExpiration)) {
			tokenEndpointResponse.addProperty("expires_in", Integer.parseInt(accessTokenExpiration));
		}

		env.putObject("token_endpoint_response", tokenEndpointResponse);

		logSuccess("Created token endpoint response", tokenEndpointResponse);

		return env;

	}

}
