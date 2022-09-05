package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveStateFromAuthorizationEndpointResponseParams extends AbstractCondition {


	@Override
	@PreEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	@PostEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);
		response.remove(CreateAuthorizationEndpointResponseParams.STATE);
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, response);
		logSuccess("Removed state from authorization endpoint response params", args( CreateAuthorizationEndpointResponseParams.ENV_KEY, response));
		return env;
	}

}
