package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.testmodule.Environment;

public class AddSessionStateToAuthorizationEndpointResponseParams extends AbstractCondition {


	@Override
	@PreEnvironment(required = {CreateAuthorizationEndpointResponseParams.ENV_KEY, "session_state_data"})
	@PostEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);

		String sessionState = env.getString("session_state_data", "session_state");

		params.addProperty("session_state", sessionState);

		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, params);

		log("Added session_state to authorization endpoint response params", args(CreateAuthorizationEndpointResponseParams.ENV_KEY, params));

		return env;

	}

}
