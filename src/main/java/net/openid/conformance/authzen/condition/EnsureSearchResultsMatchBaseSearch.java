package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Compares the results outside the fixture, those that no expected result of
 * {@code authzen_search_endpoint_expected_response} identifies, with the same results of the base
 * search captured in {@code authzen_search_base_results}: the same request without the one member
 * the module adds, named in {@code authzen_search_base_change}. Certification scenarios 7.2.3 and
 * 7.3.3 ask for identical result sets.
 *
 * <p>The results are compared as a multiset, each without its {@code properties}; a difference
 * in properties alone is logged. The two searches are separate requests, and a PDP may have
 * policies beyond the fixture (certification scenario 4.6) whose results change between them, for
 * example under a time-dependent policy, so the caller invokes this at WARNING.
 * {@link EnsureSearchFixtureEntitiesMatchBaseSearch} compares the fixture entities at FAILURE.
 */
public class EnsureSearchResultsMatchBaseSearch extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_search_endpoint_expected_response", "authzen_search_base_results",
		"authzen_search_endpoint_aggregated_results"}, strings = "authzen_search_base_change")
	public Environment evaluate(Environment env) {
		String change = env.getString("authzen_search_base_change");
		JsonArray fixture = getResults(env.getObject("authzen_search_endpoint_expected_response"), "fixture_entities");
		JsonArray baseResults = SearchResultMultiset.withoutIdentified(
			getResults(env.getObject("authzen_search_base_results"), "base_search"), fixture);
		JsonArray results = SearchResultMultiset.withoutIdentified(
			getResults(env.getObject("authzen_search_endpoint_aggregated_results"), "this_search"), fixture);

		if (!SearchResultMultiset.sameEntities(baseResults, results)) {
			throw error("The results outside the fixture differ from those of the same search without " + change
				+ ". The certification scenario asks for identical results, but these results may have changed between "
				+ "the two requests, so the difference is reported rather than failed",
				args("change", change, "base_search_results_outside_fixture", baseResults, "results_outside_fixture", results));
		}
		if (!SearchResultMultiset.sameResults(baseResults, results)) {
			log("The same results outside the fixture were returned, but the properties of some differ from the base search; "
				+ "properties are not compared", args("base_search_results_outside_fixture", baseResults, "results_outside_fixture", results));
		}
		logSuccess("The search returned the same results outside the fixture as the same search without " + change
			+ " (compared as a multiset, without properties)", args("results_outside_fixture", results));
		return env;
	}

	private JsonArray getResults(JsonObject object, String argName) {
		JsonElement results = object.get("results");
		if (results == null || !results.isJsonArray()) {
			throw error("No search results to compare", args(argName, object));
		}
		return results.getAsJsonArray();
	}
}
