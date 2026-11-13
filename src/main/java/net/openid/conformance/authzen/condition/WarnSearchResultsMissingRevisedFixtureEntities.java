package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Flags a search whose results, across all its pages, lack an entity that a not yet published
 * revision of the certification fixture adds to the expected results, such as record-2 from
 * decision rule 9 (openid/authzen#776). A PDP set up for the published fixture need not return it,
 * so this is intended to be called at WARNING; the published fixture entities are matched at
 * FAILURE by {@link EnsureAuthzenSearchResponseValsMatchExpectedVals}.
 */
public class WarnSearchResultsMissingRevisedFixtureEntities extends AbstractCondition {

	private final JsonArray revisedEntities;

	/** @param revisedEntitiesJson a JSON array of the entities the revised fixture adds */
	public WarnSearchResultsMissingRevisedFixtureEntities(String revisedEntitiesJson) {
		this.revisedEntities = JsonParser.parseString(revisedEntitiesJson).getAsJsonArray();
	}

	@Override
	@PreEnvironment(required = "authzen_search_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement resultsElement = env.getElementFromObject("authzen_search_endpoint_response", "results");
		if (resultsElement == null || !resultsElement.isJsonArray()) {
			throw error("The search response has no `results` array to check",
				args("response", env.getObject("authzen_search_endpoint_response")));
		}
		JsonArray results = resultsElement.getAsJsonArray();
		JsonArray missing = new JsonArray();
		for (JsonElement entity : revisedEntities) {
			if (SearchResultMultiset.countIdentifying(results, entity) == 0) {
				missing.add(entity);
			}
		}
		if (!missing.isEmpty()) {
			throw error("The search results lack an entity that the revised certification fixture adds to this search. "
				+ "A PDP set up for the published fixture need not return it, so this is reported rather than failed until "
				+ "the revision is published",
				args("missing_entities", missing, "results", results));
		}
		logSuccess("The search results include the entities the revised certification fixture adds to this search",
			args("revised_entities", revisedEntities));
		return env;
	}
}
