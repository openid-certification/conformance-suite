package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public class CheckBackchannelUserCodeParameterSupported extends AbstractCondition {

	public CheckBackchannelUserCodeParameterSupported(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement element = env.getElementFromObject("server", "backchannel_user_code_parameter_supported");
		if (element == null || element.isJsonObject()) {
			throw error("backchannel_user_code_parameter_supported in server was missing");
		}

		if (!element.getAsJsonPrimitive().isBoolean()) {
			throw error("Type of backchannel_user_code_parameter_supported must be boolean.");
		}

		if (!OIDFJSON.getBoolean(element)) {
			throw error("backchannel_user_code_parameter_supported must be 'true'", args("actual", OIDFJSON.getBoolean(element)));
		}

		logSuccess("backchannel_user_code_parameter_supported was 'true'");

		return env;
	}
}
