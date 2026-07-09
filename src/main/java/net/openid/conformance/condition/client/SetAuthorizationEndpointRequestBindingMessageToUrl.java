package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetAuthorizationEndpointRequestBindingMessageToUrl extends AbstractCondition {

	public static final String URL_BINDING_MESSAGE = "Review consent at https://example.test/consent";

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		authorizationEndpointRequest.addProperty("binding_message", URL_BINDING_MESSAGE);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Set binding_message in authorization endpoint request to a URL-containing value",
			args("binding_message", URL_BINDING_MESSAGE));

		return env;
	}
}
