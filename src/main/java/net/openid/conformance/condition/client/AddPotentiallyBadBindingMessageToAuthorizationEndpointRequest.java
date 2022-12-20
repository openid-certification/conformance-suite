package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddPotentiallyBadBindingMessageToAuthorizationEndpointRequest extends AbstractCondition {

	public static final String POTENTIALLY_BAD_BINDING_MESSAGE = "1234 \uD83D\uDC4D\uD83C\uDFFF 品川 Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."; // the unicode escapes are some emojis with combining characters

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		authorizationEndpointRequest.addProperty("binding_message", POTENTIALLY_BAD_BINDING_MESSAGE);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added binding message to authorization endpoint request", args("binding_message", POTENTIALLY_BAD_BINDING_MESSAGE));

		return env;
	}

}
