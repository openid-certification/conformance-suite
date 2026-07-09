package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.BindingMessageUtils;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureBackchannelRequestObjectBindingMessageDoesNotContainUrl extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_request_object")
	public Environment evaluate(Environment env) {
		JsonElement bindingMessageElement = env.getElementFromObject("backchannel_request_object", "claims.binding_message");
		if (bindingMessageElement == null) {
			logSuccess("Backchannel request object does not contain optional binding_message");
			return env;
		}

		if (!bindingMessageElement.isJsonPrimitive() || !bindingMessageElement.getAsJsonPrimitive().isString()) {
			throw error("binding_message must be a string when present",
				args("binding_message_is_string", false));
		}

		String bindingMessage = OIDFJSON.getString(bindingMessageElement);
		if (BindingMessageUtils.containsUrl(bindingMessage)) {
			throw error("binding_message must not contain URLs for Open Finance Brasil CIBA",
				args("binding_message_contains_url", true, "binding_message_length", bindingMessage.length()));
		}

		logSuccess("binding_message does not contain URLs",
			args("binding_message_contains_url", false, "binding_message_length", bindingMessage.length()));
		return env;
	}
}
