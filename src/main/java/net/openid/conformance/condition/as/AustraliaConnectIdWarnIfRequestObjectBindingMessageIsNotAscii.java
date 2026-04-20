package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.nio.charset.StandardCharsets;

public class AustraliaConnectIdWarnIfRequestObjectBindingMessageIsNotAscii extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {
		JsonElement bindingMessageElement = env.getElementFromObject("authorization_request_object", "claims.binding_message");

		if (bindingMessageElement == null) {
			throw error("'binding_message' in request object is required but not present");
		}
		if (!bindingMessageElement.isJsonPrimitive() || !bindingMessageElement.getAsJsonPrimitive().isString()) {
			throw error("'binding_message' in request object is not a string");
		}

		String bindingMessage = OIDFJSON.getString(bindingMessageElement);
		if (!StandardCharsets.US_ASCII.newEncoder().canEncode(bindingMessage)) {
			throw error("'binding_message' contains non-ASCII characters", args("binding_message", bindingMessage));
		}

		logSuccess("'binding_message' contains only ASCII characters", args("binding_message", bindingMessage));
		return env;
	}
}
