package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.BindingMessageUtils;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonElement bindingMessageElement = env.getElementFromObject("authorization_endpoint_request", "binding_message");
		if (bindingMessageElement == null) {
			logSuccess("authorization endpoint request does not contain binding_message");
			return env;
		}

		if (!bindingMessageElement.isJsonPrimitive() || !bindingMessageElement.getAsJsonPrimitive().isString()) {
			throw error("'binding_message' in authorization endpoint request is not a string",
				args("binding_message_is_string", false));
		}

		String bindingMessage = OIDFJSON.getString(bindingMessageElement);
		if (BindingMessageUtils.containsUrl(bindingMessage)) {
			throw error("'binding_message' in authorization endpoint request contains a URL",
				args("binding_message_contains_url", true, "binding_message_length", bindingMessage.length()));
		}

		logSuccess("'binding_message' in authorization endpoint request does not contain a URL",
			args("binding_message_contains_url", false, "binding_message_length", bindingMessage.length()));
		return env;
	}
}
