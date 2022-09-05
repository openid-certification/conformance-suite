package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public abstract class AbstractValidateJsonBoolean extends AbstractCondition {

	public Environment validate(Environment env, String environmentVariable, boolean defaultValue, boolean requiredValue) {

		JsonElement parameterValue = env.getElementFromObject("server", environmentVariable);
		String errorMessage = null;

		if (parameterValue == null) {
			if (defaultValue != requiredValue) {
				errorMessage = "'" + environmentVariable + "' should be '" + requiredValue + "', but is absent and the default value is '" + defaultValue + "'.";
			}
		} else {
			if (parameterValue.isJsonPrimitive()) {
				if (!parameterValue.getAsJsonPrimitive().isBoolean()) {
					errorMessage = environmentVariable + ": incorrect type, must be a boolean.";
				} else if (OIDFJSON.getBoolean(parameterValue) != requiredValue) {
					errorMessage = environmentVariable + " must be: " + requiredValue;
				}
			} else {
				errorMessage = environmentVariable + ": incorrect type, must be a boolean.";
			}
		}

		if (errorMessage != null) {
			throw error(errorMessage, args("discovery_metadata_key", environmentVariable, "expected", requiredValue, "actual", parameterValue));
		}

		logSuccess(environmentVariable + " has correct value", args(environmentVariable, parameterValue));

		return env;
	}

}
