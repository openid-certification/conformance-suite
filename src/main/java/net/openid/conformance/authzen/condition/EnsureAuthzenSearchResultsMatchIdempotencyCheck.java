package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Asserts that the search results gathered across every page of the current iteration of an
 * idempotency test loop are the ones {@link CaptureAuthzenSearchResultsForIdempotencyCheck}
 * captured on the first iteration.
 *
 * <p>Only the answer to the search is compared: the entities in the {@code results} of all pages
 * together, as a multiset (order-independent but multiplicity-preserving). Each result is
 * compared without its {@code properties}, which describe the entity rather than identify it
 * (Section 5) and may carry values such as timestamps that change between identical requests;
 * a difference there is logged, not reported. How the PDP splits the results into pages, its
 * opaque page tokens, {@code page.total} (the count at the time of the request, Section
 * 8.2.2) and {@code context} (free-form information such as execution time) are not compared.
 */
public class EnsureAuthzenSearchResultsMatchIdempotencyCheck extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_search_endpoint_aggregated_results", "authzen_idempotency_first_search_results"})
	public Environment evaluate(Environment env) {
		JsonArray expectedResults = getResults(env.getObject("authzen_idempotency_first_search_results"), "first_iteration");
		JsonArray actualResults = getResults(env.getObject("authzen_search_endpoint_aggregated_results"), "current_iteration");

		if (!SearchResultMultiset.sameEntities(expectedResults, actualResults)) {
			throw error("Search results changed across consecutive identical requests — PDP is not idempotent",
				args("first_iteration_count", expectedResults.size(), "current_iteration_count", actualResults.size(),
					"first_iteration_results", expectedResults, "current_iteration_results", actualResults));
		}
		if (!SearchResultMultiset.sameResults(expectedResults, actualResults)) {
			log("The same entities were returned, but the properties of some differ from the first iteration; "
				+ "properties are not compared",
				args("first_iteration_results", expectedResults, "current_iteration_results", actualResults));
		}

		logSuccess("Search results matched first iteration (compared as a multiset, without properties)", args("results", actualResults));
		return env;
	}

	private JsonArray getResults(JsonObject aggregated, String argName) {
		JsonElement results = aggregated.get("results");
		if (results == null || !results.isJsonArray()) {
			throw error("No search results to compare", args(argName, aggregated));
		}
		return results.getAsJsonArray();
	}
}
