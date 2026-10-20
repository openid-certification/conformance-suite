package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Asserts that a search returned the same results, across all its pages, as the base search
 * captured in {@code authzen_search_base_results}: the same request without the one member the
 * module adds, named in {@code authzen_search_base_change}. Certification scenario 7.2.3 and
 * 7.3.3 require the two result sets to be identical.
 *
 * <p>The results are compared as a multiset, each without its {@code properties}; a difference
 * in properties alone is logged. Section 8.1 lets search results depend on variables such as
 * time, so the caller invokes this at WARNING, as it does the search idempotency check.
 */
public class EnsureSearchResultsMatchBaseSearch extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_search_base_results", "authzen_search_endpoint_aggregated_results"}, strings = "authzen_search_base_change")
	public Environment evaluate(Environment env) {
		String change = env.getString("authzen_search_base_change");
		JsonArray baseResults = getResults(env.getObject("authzen_search_base_results"), "base_search");
		JsonArray results = getResults(env.getObject("authzen_search_endpoint_aggregated_results"), "this_search");

		if (!SearchResultMultiset.sameEntities(baseResults, results)) {
			throw error("The search returned different results from the same search without " + change
				+ "; the certification scenario requires the result sets to be identical",
				args("change", change, "base_search_results", baseResults, "results", results));
		}
		if (!SearchResultMultiset.sameResults(baseResults, results)) {
			log("The same entities were returned, but the properties of some differ from the base search; "
				+ "properties are not compared", args("base_search_results", baseResults, "results", results));
		}
		logSuccess("The search returned the same results as the same search without " + change
			+ " (compared as a multiset, without properties)", args("results", results));
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
