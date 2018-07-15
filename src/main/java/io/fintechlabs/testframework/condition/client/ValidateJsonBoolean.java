package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateJsonBoolean extends AbstractCondition {

	public ValidateJsonBoolean(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	public Environment validate(Environment env, String environmentVariable,
			Boolean defaultValue, Boolean requiredValue) {

		JsonElement parameterValue = env.findElement("server", environmentVariable);
		String errorMessage = null;

		if (parameterValue == null) {
			if (defaultValue != requiredValue) {
				errorMessage = "'" + environmentVariable + "' should be '" + requiredValue + "', but is absent and the default value is '"+defaultValue+"'.";
			}
		} else {
			if (parameterValue.isJsonPrimitive()) {
				if (parameterValue.getAsBoolean() != requiredValue) {
					errorMessage = environmentVariable + " must be: " + requiredValue;
				}
			} else {
				errorMessage = environmentVariable + ": incorrect type, must be a boolean.";
			}
		}

		if (errorMessage != null) {
			throw error(errorMessage, args("discovery metadata key", environmentVariable, "expected", requiredValue, "actual", parameterValue));
		}

		logSuccess(environmentVariable, args(environmentVariable, parameterValue));

		return env;
	}

	@Override
	public Environment evaluate(Environment env) {
		// TODO Auto-generated method stub
		return null;
	}

}
