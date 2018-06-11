package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateJsonArray extends AbstractCondition {

	public ValidateJsonArray(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	public Environment validate(Environment env, String environmentVariable, String environmentVariableText,
			String[] setValues, Integer minimumMatchesRequired, String errorMessageWhenNull,
			String errorMessageNonArray, String errorMessageNotEnough) {

		JsonElement serverValues = env.findElement("server", environmentVariable);
		String errorMessage = null;
		int foundCount = 0;

		if (serverValues == null) {
			errorMessage = errorMessageWhenNull;
		} else {

			if (!serverValues.isJsonArray()) {
				errorMessage = errorMessageNonArray;
			} else {

				int viableSize = setValues.length;
				int serverSize = serverValues.getAsJsonArray().size();

				JsonArray serverData = serverValues.getAsJsonArray();

				for (int viableIndex = 0; viableIndex < viableSize; viableIndex++) {
					for (int serverIndex = 0; serverIndex < serverSize; serverIndex++) {
						if (setValues[viableIndex].equals(serverData.get(serverIndex).getAsString())) {
							foundCount++;
							break;
						}
					}
				}

				if (foundCount < minimumMatchesRequired) {
					errorMessage = errorMessageNotEnough;
				}
			}
		}

		if (errorMessage != null) {
			throw error(errorMessage, args("expected", stringArrayToString(setValues), "actual", serverValues));
		}

		logSuccess(environmentVariableText, args("actual", serverValues));

		return env;
	}

	private String stringArrayToString(String[] array) {

		StringBuilder sb = new StringBuilder();
		sb.append("[");

		for (Object obj : array)
			sb.append("\"" + obj.toString() + "\"" + ",");

		String out = sb.substring(0, sb.length() - 1);
		return out + "]";
	}

	@Override
	public Environment evaluate(Environment env) {
		// TODO Auto-generated method stub
		return null;
	}
}
