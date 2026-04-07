package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class EnsureAuthzenSearchResponseValsMatchExpectedVals extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_search_endpoint_expected_response", "authzen_search_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonArray expected = env.getElementFromObject("authzen_search_endpoint_expected_response", "results").getAsJsonArray();
		JsonArray results = env.getElementFromObject("authzen_search_endpoint_response", "results").getAsJsonArray();
		if (expected.size() != results.size()) {
			throw error("The number of results elements in the response does not match expected",
				args("expected", expected.size(), "actual", results.size()));
		}

		List<JsonElement> expectedList = expected.deepCopy().asList();
		List<JsonElement> resultsList = results.deepCopy().asList();

		resultsList.removeAll(expectedList);
		if(!resultsList.isEmpty()) { // should not contain any elements after removing all expected elements
			expectedList.removeAll(results.asList());
			// resultsList now contains unexpected elements
			// expectedList now contains missing elements not returned
			if(!expectedList.isEmpty() && !resultsList.isEmpty()) {
				throw error("Search results do not match", args("missing expected elements", expectedList, "unexpected elements", resultsList));
			} else if(!resultsList.isEmpty()) {
				throw error("Search results contains unexpected elements", args("unexpected elements", resultsList));
			} else if(!expectedList.isEmpty()) {
				throw error("Search results is missing expected elements", args("missing expected elements", expectedList));
			}
		}

		logSuccess("The search response match expected values");
		return env;
	}

}
