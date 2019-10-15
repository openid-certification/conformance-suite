package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckBackchannelUserCodeParameterSupported extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement element = env.getElementFromObject("server", "backchannel_user_code_parameter_supported");
		if (element == null) {
			logSuccess("backchannel_user_code_parameter_supported is not present");
			return env;
		}

		if (element.isJsonObject() || !element.getAsJsonPrimitive().isBoolean()) {
			throw error("Type of backchannel_user_code_parameter_supported must be boolean.");
		}

		logSuccess("backchannel_user_code_parameter_supported is a valid boolean");

		return env;
	}
}
