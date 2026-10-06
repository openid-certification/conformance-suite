package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Section 8.2.1 defines a request's `page.limit` as "the maximum number of results to return
 * in the response". Pagination is optional for the PDP (Section 8.2), and a PDP that does not
 * paginate returns the whole result set in one response and ignores the page object
 * (certification scenario 7.5.4). The limit is therefore only checked when the PDP is
 * paginating this search: the request carried a page token, or the response returns a
 * non-empty next_token. No sentence makes the limit a MUST; the suite reads "maximum" as binding
 * on a PDP that paginates, and an excess fails (openid/authzen#764 asks).
 */
public class EnsureSearchResponseResultsWithinPageLimit extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_api_endpoint_request", "authzen_search_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonElement limit = env.getElementFromObject("authzen_api_endpoint_request", "page.limit");
		if (limit == null) {
			log("The search request did not set page.limit");
			return env;
		}
		if (!limit.isJsonPrimitive() || !limit.getAsJsonPrimitive().isNumber()) {
			log("The search request's page.limit is not a number, so the response size is not checked against it", args("limit", limit));
			return env;
		}
		JsonObject searchResponse = env.getObject("authzen_search_endpoint_response");
		int resultsSize = searchResponse.getAsJsonArray("results").size();
		boolean requestHadToken = isNonEmptyString(env.getElementFromObject("authzen_api_endpoint_request", "page.token"));
		boolean responseHasNextToken = isNonEmptyString(env.getElementFromObject("authzen_search_endpoint_response", "page.next_token"));
		if (!requestHadToken && !responseHasNextToken) {
			log("The PDP returned the search in a single response, so it is not paginating this search and page.limit does not apply",
				args("limit", limit, "results_size", resultsSize));
			return env;
		}
		if (resultsSize > OIDFJSON.getInt(limit)) {
			throw error("The PDP is paginating this search but the response contains more results than the request's page.limit",
				args("limit", limit, "results_size", resultsSize));
		}
		logSuccess("The search response contains no more results than the request's page.limit",
			args("limit", limit, "results_size", resultsSize));
		return env;
	}

	private static boolean isNonEmptyString(JsonElement elem) {
		return elem != null
			&& elem.isJsonPrimitive()
			&& elem.getAsJsonPrimitive().isString()
			&& !OIDFJSON.getString(elem).isEmpty();
	}
}
