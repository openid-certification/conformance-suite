package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

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
			ensureValidResponseResultsObject(elem.getAsJsonObject());
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

	// Check individual results element if needed
	protected void ensureValidResponseResultsObject(JsonObject resultsObj) {
		// Nothing to do here
	}

	/**
	 * Returns the value of a member that Section 5 defines as a REQUIRED string, failing
	 * if it is absent or not a string. {@code entity} names the result object in
	 * messages, e.g. "A subject object".
	 */
	protected String getRequiredStringMember(JsonObject resultsObj, String member, String entity) {
		JsonElement elem = resultsObj.get(member);
		if (elem == null) {
			throw error(entity + " in the results array does not contain `" + member + "`", args("element", resultsObj));
		}
		if (!elem.isJsonPrimitive() || !elem.getAsJsonPrimitive().isString()) {
			throw error(entity + " in the results array has a `" + member + "` that is not a string", args("element", resultsObj));
		}
		return OIDFJSON.getString(elem);
	}

	/**
	 * Fails if the OPTIONAL `properties` member (Section 5) is present but not an object.
	 */
	protected void ensurePropertiesIsObject(JsonObject resultsObj, String entity) {
		JsonElement properties = resultsObj.get("properties");
		if (properties != null && !properties.isJsonObject()) {
			throw error(entity + " in the results array has `properties` that is not an object", args("element", resultsObj));
		}
	}

}
