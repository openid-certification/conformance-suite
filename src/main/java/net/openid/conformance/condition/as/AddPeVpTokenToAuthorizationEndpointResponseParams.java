package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddPeVpTokenToAuthorizationEndpointResponseParams extends AbstractCondition {

	@Override
	@PreEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY, strings = "credential")
	@PostEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);

		String vpToken = env.getString("credential");

		params.addProperty("vp_token", vpToken);

		logSuccess("Added credential in 'vp_token' authorization endpoint response parameter", args(CreateAuthorizationEndpointResponseParams.ENV_KEY, params));

		return env;

	}

}
