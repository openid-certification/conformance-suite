package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddStateToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "state", required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		String state = env.getString("state");
		if (Strings.isNullOrEmpty(state)) {
			throw error("Couldn't find state value");
		}

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		authorizationEndpointRequest.addProperty("state", state);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added state parameter to request", authorizationEndpointRequest);

		return env;
	}

}
