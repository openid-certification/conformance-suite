package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.HashSet;
import java.util.Set;

public class EnsureSearchPageResultsAreSubsetOfExpected extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_search_endpoint_expected_response", "authzen_search_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonArray expected = env.getElementFromObject("authzen_search_endpoint_expected_response", "results").getAsJsonArray();
		JsonArray pageResults = env.getElementFromObject("authzen_search_endpoint_response", "results").getAsJsonArray();

		Set<JsonElement> expectedSet = new HashSet<>(expected.asList());
		Set<JsonElement> unexpected = new HashSet<>();
		for (JsonElement elem : pageResults) {
			if (!expectedSet.contains(elem)) {
				unexpected.add(elem);
			}
		}

		if (!unexpected.isEmpty()) {
			throw error("Search page contains results not in the expected set",
				args("unexpected elements", unexpected, "expected set", expectedSet));
		}

		logSuccess("Search page results are a subset of expected results");
		return env;
	}
}
