package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Asserts that each fixture entity, the expected results of
 * {@code authzen_search_endpoint_expected_response}, appears as often in this search's results,
 * across all its pages, as in the base search captured in {@code authzen_search_base_results}: the
 * same request without the one member the module adds, named in {@code authzen_search_base_change}.
 *
 * <p>For a search with a context, certification scenario 4.6 says the fixture decisions "MUST hold
 * regardless of whether context is present or absent", and also lets a PDP have "additional
 * subjects, resources, actions, and policies beyond the fixture", which a context may legitimately
 * affect. So only the fixture entities are compared; other results may differ. Section 8.1 lets
 * search results depend on variables such as time, so the caller invokes this at WARNING.
 */
public class EnsureSearchFixtureEntitiesMatchBaseSearch extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_search_endpoint_expected_response", "authzen_search_base_results",
		"authzen_search_endpoint_aggregated_results"}, strings = "authzen_search_base_change")
	public Environment evaluate(Environment env) {
		String change = env.getString("authzen_search_base_change");
		JsonArray fixture = getResults(env.getObject("authzen_search_endpoint_expected_response"), "fixture_entities");
		JsonArray baseResults = getResults(env.getObject("authzen_search_base_results"), "base_search");
		JsonArray results = getResults(env.getObject("authzen_search_endpoint_aggregated_results"), "this_search");

		JsonArray differing = new JsonArray();
		for (JsonElement entity : fixture) {
			if (SearchResultMultiset.countIdentifying(baseResults, entity) != SearchResultMultiset.countIdentifying(results, entity)) {
				differing.add(entity);
			}
		}
		if (!differing.isEmpty()) {
			throw error("The fixture entities in the results differ from those of the same search without " + change
				+ "; certification scenario 4.6 says the fixture decisions hold whether or not a context is present",
				args("change", change, "differing_fixture_entities", differing, "base_search_results", baseResults, "results", results));
		}
		logSuccess("The search returned the fixture entities as the same search without " + change
			+ " did; results outside the fixture are not compared", args("fixture_entities", fixture, "results", results));
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
