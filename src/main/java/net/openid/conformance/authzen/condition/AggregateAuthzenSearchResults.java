package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AggregateAuthzenSearchResults extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_search_endpoint_response")
	@PostEnvironment(required = "authzen_search_endpoint_aggregated_results")
	public Environment evaluate(Environment env) {
		JsonElement resultsElem = env.getElementFromObject("authzen_search_endpoint_response", "results");
		if (resultsElem == null || !resultsElem.isJsonArray()) {
			throw error("Search response has no results array to aggregate", args("response", env.getObject("authzen_search_endpoint_response")));
		}
		JsonArray pageResults = resultsElem.getAsJsonArray();

		JsonObject aggregated = env.getObject("authzen_search_endpoint_aggregated_results");
		JsonArray accumulator;
		if (aggregated == null) {
			aggregated = new JsonObject();
			accumulator = new JsonArray();
			aggregated.add("results", accumulator);
			env.putObject("authzen_search_endpoint_aggregated_results", aggregated);
		} else {
			accumulator = aggregated.getAsJsonArray("results");
		}

		accumulator.addAll(pageResults);

		logSuccess("Aggregated search results across pages",
			args("this page count", pageResults.size(), "aggregated count", accumulator.size()));
		return env;
	}
}
