package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateJsonBoolean extends AbstractCondition {
	
	public ValidateJsonBoolean(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}
	
	public Environment validate(Environment env, String environmentVariable, String environmentVariableText,
			Boolean requiredValue) {

		JsonElement parameterValue = env.findElement("server", environmentVariable);
		String errorMessage = null;

		if (parameterValue == null) {
			errorMessage = environmentVariableText + "Should be '" + requiredValue + "'. Is currently NULL";
		} else {
			if (parameterValue.isJsonPrimitive()) {
				if (parameterValue.getAsBoolean() != requiredValue) {
					errorMessage = environmentVariable + " must be: " + requiredValue;
				}
			} else {
				errorMessage = environmentVariable + ": incorrect type. Should be JsonPrimitive and boolean.";
			}
		}

		if (errorMessage != null) {
			throw error(errorMessage, args("expected", requiredValue, "actual", parameterValue));
		}

		logSuccess(environmentVariableText, args(environmentVariable, parameterValue));

		return env;
	}

	@Override
	public Environment evaluate(Environment env) {
		// TODO Auto-generated method stub
		return null;
	}

}
