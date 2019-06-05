package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddClientNotificationTokenToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request", strings = "client_notification_token" )
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String token = env.getString("client_notification_token");

		authorizationEndpointRequest.addProperty("client_notification_token", token);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added client_notification_token '"+token+"' to authorization endpoint request", authorizationEndpointRequest);

		return env;
	}

}
