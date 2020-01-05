package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddCodeToAuthorizationEndpointResponseParams extends AbstractCondition {


	@Override
	@PreEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY, strings = "authorization_code")
	@PostEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);

		String code = env.getString("authorization_code");

		params.addProperty("code", code);

		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, params);

		logSuccess("Added code to authorization endpoint response params", args(CreateAuthorizationEndpointResponseParams.ENV_KEY, params));

		return env;

	}

}
