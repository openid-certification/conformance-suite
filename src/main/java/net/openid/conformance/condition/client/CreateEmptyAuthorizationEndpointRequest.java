package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateEmptyAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = new JsonObject();

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Created empty authorization endpoint request", authorizationEndpointRequest);

		return env;

	}

}
