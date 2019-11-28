package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddTokenToAuthorizationEndpointResponseParams extends AbstractCondition {

	private static final String AUTHORIZATION_ENDPOINT_RESPONSE_PARAMS = "authorization_endpoint_response_params";
	@Override
	@PreEnvironment(required = AUTHORIZATION_ENDPOINT_RESPONSE_PARAMS, strings = {"access_token", "token_type"})
	@PostEnvironment(required = AUTHORIZATION_ENDPOINT_RESPONSE_PARAMS)
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(AUTHORIZATION_ENDPOINT_RESPONSE_PARAMS);

		String accessToken = env.getString("access_token");
		String tokenType = env.getString("token_type");

		params.addProperty("access_token", accessToken);
		params.addProperty("token_type", tokenType);

		env.putObject(AUTHORIZATION_ENDPOINT_RESPONSE_PARAMS, params);

		logSuccess("Added token and token_type to authorization endpoint response params", args(AUTHORIZATION_ENDPOINT_RESPONSE_PARAMS, params));

		return env;

	}

}
