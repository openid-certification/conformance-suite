package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddTokenToAuthorizationEndpointResponseParams extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response_params", strings = {"access_token", "token_type"})
	@PostEnvironment(required = "authorization_endpoint_response_params")
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject("authorization_endpoint_response_params");

		String accessToken = env.getString("access_token");
		String tokenType = env.getString("token_type");

		params.addProperty("access_token", accessToken);
		params.addProperty("token_type", tokenType);

		env.putObject("authorization_endpoint_response_params", params);

		logSuccess("Added token and token_type to authorization endpoint response params", args("authorization_endpoint_response_params", params));

		return env;

	}

}
