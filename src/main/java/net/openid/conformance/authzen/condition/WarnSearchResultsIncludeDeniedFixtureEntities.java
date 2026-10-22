package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Section 8.1: "any result from a Search API, when subsequently used in an Access Evaluation API
 * call, SHOULD result in a "decision": true response". The certification fixture (scenario 4.4)
 * makes some entities denied for a search, such as bob for writing record-1 (decision rule 4), so
 * a search result naming one is flagged. The harness does not reject other additional results
 * (scenario 4.5), which the PDP's own policies may permit. Intended to be called at WARNING.
 * Entities are identified as in {@link EnsureAuthzenSearchResponseValsMatchExpectedVals}, by the
 * members the denied entity gives.
 */
public class WarnSearchResultsIncludeDeniedFixtureEntities extends AbstractCondition {

	private final JsonArray deniedEntities;

	/** {@code deniedEntitiesJson} is a JSON array of the entities the fixture denies for this search. */
	public WarnSearchResultsIncludeDeniedFixtureEntities(String deniedEntitiesJson) {
		this.deniedEntities = JsonParser.parseString(deniedEntitiesJson).getAsJsonArray();
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
		JsonArray found = new JsonArray();
		for (JsonElement denied : deniedEntities) {
			if (SearchResultMultiset.countIdentifying(results, denied) > 0) {
				found.add(denied);
			}
		}
		if (!found.isEmpty()) {
			throw error("The search results include an entity the certification fixture denies for this search, "
				+ "so an Access Evaluation API call with it would not return a true decision",
				args("denied_entities_returned", found, "results", results));
		}
		logSuccess("The search results include no entity the certification fixture denies for this search",
			args("denied_entities", deniedEntities));
		return env;
	}
}
