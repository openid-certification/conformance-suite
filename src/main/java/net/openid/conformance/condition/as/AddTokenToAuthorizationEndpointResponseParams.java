package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddTokenToAuthorizationEndpointResponseParams extends AbstractCondition {


	@Override
	@PreEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY, strings = {"access_token", "token_type"})
	@PostEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);

		String accessToken = env.getString("access_token");
		String tokenType = env.getString("token_type");

		params.addProperty("access_token", accessToken);
		params.addProperty("token_type", tokenType);

		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, params);

		log("Added token and token_type to authorization endpoint response params", args(CreateAuthorizationEndpointResponseParams.ENV_KEY, params));

		return env;

	}

}
