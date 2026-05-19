package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureValidSearchResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_search_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject searchResponse = env.getObject("authzen_search_endpoint_response");
		JsonElement resultsElem = searchResponse.get("results");
		if (null == resultsElem) {
			throw error("No results element in search API response", args("authzen_search_endpoint_response", searchResponse));
		}
		if(!resultsElem.isJsonArray()) {
			throw error("Results in API Search response is not an array", args("authzen_search_endpoint_response", searchResponse));
		}
		for(JsonElement elem : resultsElem.getAsJsonArray()) {
			if(!elem.isJsonObject()) {
				throw error("An element in the results array is not an object", args("Element", elem));
			}
		}

		if(searchResponse.has("context")) {
			if(!searchResponse.get("context").isJsonObject()) {
				throw error("The context element in the search response not an object", args("authzen_search_endpoint_response", searchResponse));
			}
		}

		// page will be checked by EnsureValidSearchResponsePage
		logSuccess("Search response is valid");
		return env;

	}

}
