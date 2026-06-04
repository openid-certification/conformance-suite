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
		JsonArray expected = readResultsArray(env, "authzen_search_endpoint_expected_response", "expected");
		JsonArray results = readResultsArray(env, "authzen_search_endpoint_response", "actual");

		// Per the certification spec, the harness validates that the expected entities appear in the
		// results but does not reject additional results. Use a subset check: expected MUST be a
		// subset of actual.
		Set<JsonElement> expectedSet = new HashSet<>(expected.asList());
		Set<JsonElement> resultsSet = new HashSet<>(results.asList());

		expectedSet.removeAll(resultsSet);
		// expectedSet now contains missing elements not returned

		if(!expectedSet.isEmpty()){
			throw error("Search result is missing expected elements", args("missing expected elements", expectedSet));
		}

		logSuccess("The search response contains all expected values");
		return env;
	}

	private JsonArray readResultsArray(Environment env, String objectKey, String role) {
		JsonElement elem = env.getElementFromObject(objectKey, "results");
		if (elem == null) {
			throw error(role + " search response has no `results` array", args("env_key", objectKey));
		}
		if (!elem.isJsonArray()) {
			throw error(role + " search response `results` is not an array",
				args("env_key", objectKey, "value", elem));
		}
		return elem.getAsJsonArray();
	}
}
