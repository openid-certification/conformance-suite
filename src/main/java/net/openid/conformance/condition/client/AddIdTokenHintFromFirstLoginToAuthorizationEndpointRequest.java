package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddIdTokenHintFromFirstLoginToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "first_id_token" } )
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String firstIdToken = env.getString("first_id_token", "value");

		authorizationEndpointRequest.addProperty("id_token_hint", firstIdToken);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added id_token_hint to authorization endpoint request", authorizationEndpointRequest);

		return env;

	}

}
