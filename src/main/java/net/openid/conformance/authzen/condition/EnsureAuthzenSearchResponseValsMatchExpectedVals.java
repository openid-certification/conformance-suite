package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.HashSet;
import java.util.Set;

public class EnsureAuthzenSearchResponseValsMatchExpectedVals extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_search_endpoint_expected_response", "authzen_search_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonArray expected = env.getElementFromObject("authzen_search_endpoint_expected_response", "results").getAsJsonArray();
		JsonArray results = env.getElementFromObject("authzen_search_endpoint_response", "results").getAsJsonArray();

		// Expected and actual responses should not contain duplicate values, but use Set operations just in case
		Set<JsonElement> expectedList = new HashSet<>(expected.asList());
		Set<JsonElement> resultsList = new HashSet<>(results.asList());

		resultsList.removeAll(expectedList);
		expectedList.removeAll(results.asList());
		// resultsList now contains unexpected elements
		// expectedList now contains missing elements not returned

		if(!resultsList.isEmpty()) { // should not contain any elements after removing all expected elements
			if(!expectedList.isEmpty()) {
				throw error("Search result does not match", args("missing expected elements", expectedList, "unexpected elements", resultsList));
			} else {
				throw error("Search result contains unexpected elements", args("unexpected elements", resultsList));
			}
		} else if(!expectedList.isEmpty()){
			throw error("Search result is missing expected elements", args("missing expected elements", expectedList));
		}

		logSuccess("The search response match expected values");
		return env;
	}
}
