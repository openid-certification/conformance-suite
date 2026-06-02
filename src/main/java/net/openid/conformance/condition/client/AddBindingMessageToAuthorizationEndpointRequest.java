package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddBindingMessageToAuthorizationEndpointRequest extends AbstractCondition {

	public static final String DEFAULT_BINDING_MESSAGE = "1234";

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		String bindingMessage = env.getString("requested_binding_message");

		if (bindingMessage == null) {
			bindingMessage = DEFAULT_BINDING_MESSAGE;
		}

		authorizationEndpointRequest.addProperty("binding_message", bindingMessage);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added binding message to authorization endpoint request", args("binding_message", bindingMessage));

		return env;
	}

}
