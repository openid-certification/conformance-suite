package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public class ValidateJsonBoolean extends AbstractCondition {

	public Environment validate(Environment env, String environmentVariable,Boolean defaultValue, Boolean requiredValue) {

		JsonElement parameterValue = env.getElementFromObject("server", environmentVariable);
		String errorMessage = null;

		if (parameterValue == null) {
			if (defaultValue != requiredValue) {
				errorMessage = "'" + environmentVariable + "' should be '" + requiredValue + "', but is absent and the default value is '" + defaultValue + "'.";
			}
		} else {
			if (parameterValue.isJsonPrimitive()) {
				if (OIDFJSON.getBoolean(parameterValue) != requiredValue) {
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

	@Override
	public Environment evaluate(Environment env) {
		// TODO Auto-generated method stub
		return null;
	}

}
