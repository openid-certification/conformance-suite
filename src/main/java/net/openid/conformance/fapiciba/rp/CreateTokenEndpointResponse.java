package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateTokenEndpointResponse extends AbstractCondition {

	public static final int EXPIRES_IN = 600;

	@Override
	@PreEnvironment(strings = { "access_token", "token_type" }) // note the others are optional
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

		tokenEndpointResponse.addProperty("expires_in", EXPIRES_IN);

		env.putObject("token_endpoint_response", tokenEndpointResponse);

		logSuccess("Created token endpoint response", tokenEndpointResponse);

		return env;

	}

}
