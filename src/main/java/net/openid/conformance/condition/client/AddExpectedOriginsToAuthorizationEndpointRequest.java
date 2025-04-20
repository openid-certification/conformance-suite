package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddExpectedOriginsToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "origin", required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		String origin = env.getString("origin");

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		JsonArray expectedOrigins = new JsonArray();
		expectedOrigins.add(origin);

		authorizationEndpointRequest.add("expected_origins", expectedOrigins);

		logSuccess("Added expected_origins to request", authorizationEndpointRequest);

		return env;

	}

}
