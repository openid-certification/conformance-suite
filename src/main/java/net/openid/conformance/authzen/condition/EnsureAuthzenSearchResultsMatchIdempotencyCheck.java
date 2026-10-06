package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashMap;
import java.util.Map;

/**
 * Asserts that the current Search API response returns the same results as the body
 * captured by {@link CaptureAuthzenResponseBodyForIdempotencyCheck} on the first
 * iteration of an idempotency test loop.
 *
 * <p>Only the answer to the search is compared: the {@code results} array, as a
 * multiset (order-independent but multiplicity-preserving), and whether a non-empty
 * {@code page.next_token} signals more pages. The token's value is opaque to the
 * PEP. {@code page.total} may be an estimate (Section 8.2.2), {@code page.count}
 * describes {@code results}, and {@code context} carries free-form information
 * such as execution time, so none of them is compared.
 */
public class EnsureAuthzenSearchResultsMatchIdempotencyCheck extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response", strings = "authzen_idempotency_first_response_body")
	public Environment evaluate(Environment env) {
		JsonObject expected = JsonParser.parseString(env.getString("authzen_idempotency_first_response_body")).getAsJsonObject();
		JsonElement actualElem = env.getElementFromObject("authzen_api_endpoint_response", "body_json");
		if (actualElem == null) {
			throw error("Current iteration has no JSON response body");
		}
		if (!actualElem.isJsonObject()) {
			throw error("Current iteration response body is not a JSON object",
				args("body_json", actualElem));
		}
		JsonObject actual = actualElem.getAsJsonObject();

		JsonArray expectedResults = getResults(expected, "first_iteration_body");
		JsonArray actualResults = getResults(actual, "current_iteration_body");

		if (expectedResults.size() != actualResults.size()) {
			throw error("Search results array size changed across consecutive identical requests — PDP is not idempotent",
				args("first_iteration_results", expectedResults, "current_iteration_results", actualResults));
		}
		if (!countOccurrences(expectedResults).equals(countOccurrences(actualResults))) {
			throw error("Search results changed across consecutive identical requests — PDP is not idempotent",
				args("first_iteration_results", expectedResults, "current_iteration_results", actualResults));
		}

		boolean expectedHasNextToken = hasPageNextToken(expected);
		boolean actualHasNextToken = hasPageNextToken(actual);
		if (expectedHasNextToken != actualHasNextToken) {
			throw error("Pagination signal changed across consecutive identical requests — `page.next_token` "
					+ (expectedHasNextToken ? "was present in the first iteration but is missing in the current one" : "was missing in the first iteration but is present in the current one")
					+ "; PDP is not idempotent",
				args("first_iteration_body", expected, "current_iteration_body", actual));
		}

		logSuccess("Search results matched first iteration (compared as a multiset)", args("results", actualResults));
		return env;
	}

	/**
	 * Gson's JsonObject equality and hash code ignore member order, so counting equal elements
	 * compares the arrays as multisets without depending on how members are ordered (Section
	 * 10.1.1: "Implementations MUST NOT assume a particular ordering of JSON object members").
	 */
	private static Map<JsonElement, Integer> countOccurrences(JsonArray results) {
		Map<JsonElement, Integer> counts = new HashMap<>();
		for (JsonElement result : results) {
			counts.merge(result, 1, Integer::sum);
		}
		return counts;
	}

	private JsonArray getResults(JsonObject body, String argName) {
		JsonElement results = body.get("results");
		if (results == null || !results.isJsonArray()) {
			throw error("Search response has no `results` array", args(argName, body));
		}
		return results.getAsJsonArray();
	}

	private static boolean hasPageNextToken(JsonObject body) {
		// An empty-string next_token is semantically equivalent to absent
		// (Section 8.2.2 — empty/absent both signal "no more pages"), so the
		// presence-equality check treats them as the same observable.
		if (!body.has("page") || !body.get("page").isJsonObject()) {
			return false;
		}
		JsonObject page = body.getAsJsonObject("page");
		if (!page.has("next_token")) {
			return false;
		}
		JsonElement token = page.get("next_token");
		return token.isJsonPrimitive()
			&& token.getAsJsonPrimitive().isString()
			&& !OIDFJSON.getString(token).isEmpty();
	}
}
