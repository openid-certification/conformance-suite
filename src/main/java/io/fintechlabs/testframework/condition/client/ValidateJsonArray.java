package io.fintechlabs.testframework.condition.client;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public class ValidateJsonArray extends AbstractCondition {

	public ValidateJsonArray(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	public long countMatchingElements(List<String> searchValues, JsonArray searchSpace ) {
		long foundCount = 0;

		int viableSize = searchValues.size();
		int serverSize = searchSpace.size();

		for (int viableIndex = 0; viableIndex < viableSize; viableIndex++) {
			for (int serverIndex = 0; serverIndex < serverSize; serverIndex++) {
				if (searchValues.get(viableIndex).equals(OIDFJSON.getString(searchSpace.get(serverIndex)))) {
					foundCount++;
					break;
				}
			}
		}
		return foundCount;
	}

	public Environment validate(Environment env, String environmentVariable,
			List<String> setValues, Integer minimumMatchesRequired,
			String errorMessageNotEnough) {

		JsonElement serverValues = env.getElementFromObject("server", environmentVariable);
		String errorMessage = null;

		if (serverValues == null) {
			errorMessage = environmentVariable + ": not found";
		} else {

			if (!serverValues.isJsonArray()) {
				errorMessage = "'" + environmentVariable + "' should be an array";
			} else {

				if (countMatchingElements(setValues, serverValues.getAsJsonArray()) < minimumMatchesRequired) {
					errorMessage = errorMessageNotEnough;
				}
			}
		}

		if (errorMessage != null) {
			if (minimumMatchesRequired == 1) {
				throw error(errorMessage, args("discovery_metadata_key", environmentVariable, "expected_at_least_one_of", setValues, "actual", serverValues));
			}
			throw error(errorMessage, args("discovery_metadata_key", environmentVariable, "expected", setValues, "actual", serverValues));
		}

		logSuccess(environmentVariable, args("actual", serverValues, "expected", setValues, "minimum_matches_required", minimumMatchesRequired));

		return env;
	}

	@Override
	public Environment evaluate(Environment env) {
		// TODO Auto-generated method stub
		return null;
	}
}
