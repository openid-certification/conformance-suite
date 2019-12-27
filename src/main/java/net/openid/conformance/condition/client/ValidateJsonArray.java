package net.openid.conformance.condition.client;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateJsonArray extends AbstractCondition {

	public long countMatchingElements(List<String> searchValues, JsonArray searchSpace ) {
		long foundCount = 0;

		int viableSize = searchValues.size();
		int serverSize = searchSpace.size();

		for (int viableIndex = 0; viableIndex < viableSize; viableIndex++) {
			for (int serverIndex = 0; serverIndex < serverSize; serverIndex++) {
				if (elementsEqual(searchValues.get(viableIndex), OIDFJSON.getString(searchSpace.get(serverIndex)))) {
					foundCount++;
					break;
				}
			}
		}
		return foundCount;
	}

	protected boolean elementsEqual(String e1, String e2) {
		return e1.equals(e2);
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
