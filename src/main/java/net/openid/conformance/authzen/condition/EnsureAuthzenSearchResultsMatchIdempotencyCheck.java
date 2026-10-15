package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.HashMap;
import java.util.Map;

/**
 * Asserts that the search results gathered across every page of the current iteration of an
 * idempotency test loop are the ones {@link CaptureAuthzenSearchResultsForIdempotencyCheck}
 * captured on the first iteration.
 *
 * <p>Only the answer to the search is compared: the {@code results} of all pages together, as
 * a multiset (order-independent but multiplicity-preserving). How the PDP splits them into
 * pages, its opaque page tokens, {@code page.total} (which may be an estimate, Section 8.2.2)
 * and {@code context} (free-form information such as execution time) are not compared.
 */
public class EnsureAuthzenSearchResultsMatchIdempotencyCheck extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_search_endpoint_aggregated_results", "authzen_idempotency_first_search_results"})
	public Environment evaluate(Environment env) {
		JsonArray expectedResults = getResults(env.getObject("authzen_idempotency_first_search_results"), "first_iteration");
		JsonArray actualResults = getResults(env.getObject("authzen_search_endpoint_aggregated_results"), "current_iteration");

		if (expectedResults.size() != actualResults.size()) {
			throw error("The number of search results changed across consecutive identical requests — PDP is not idempotent",
				args("first_iteration_results", expectedResults, "current_iteration_results", actualResults));
		}
		if (!countOccurrences(expectedResults).equals(countOccurrences(actualResults))) {
			throw error("Search results changed across consecutive identical requests — PDP is not idempotent",
				args("first_iteration_results", expectedResults, "current_iteration_results", actualResults));
		}

		logSuccess("Search results matched first iteration (compared as a multiset)", args("results", actualResults));
		return env;
	}

	/**
	 * Gson's JsonObject equality and hash code ignore member order, so counting equal elements
	 * compares the arrays as multisets without depending on how members are ordered (Section
	 * 10.1.1: "Implementations MUST NOT assume a particular ordering of JSON object members").
	 */
	private static Map<JsonElement, Integer> countOccurrences(JsonArray results) {
		Map<JsonElement, Integer> counts = new HashMap<>();
		for (JsonElement result : results) {
			counts.merge(result, 1, Integer::sum);
		}
		return counts;
	}

	private JsonArray getResults(JsonObject aggregated, String argName) {
		JsonElement results = aggregated.get("results");
		if (results == null || !results.isJsonArray()) {
			throw error("No search results to compare", args(argName, aggregated));
		}
		return results.getAsJsonArray();
	}
}
