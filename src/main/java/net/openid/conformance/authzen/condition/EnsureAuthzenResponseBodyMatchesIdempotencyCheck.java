package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Asserts that the JSON body of the current Authzen API response equals the body
 * captured by {@link CaptureAuthzenResponseBodyForIdempotencyCheck} on the first
 * iteration of an idempotency test loop.
 *
 * <p>Search responses (subject/resource/action) carry a {@code results} array whose
 * order is not guaranteed across calls, so {@code results} is compared as a multiset
 * (order-independent but multiplicity-preserving). The {@code page.next_token}
 * <em>value</em> is opaque to the PEP and may legitimately differ across identical
 * requests (e.g. a server that derives the token from a timestamp), so it is
 * stripped from both bodies before the strict-rest comparison. The
 * <em>presence</em> of {@code page.next_token}, however, is an observable
 * pagination signal ("are there more pages?") that MUST stay consistent across
 * identical requests — so we assert presence-equality before stripping the
 * value. The rest of the response body is compared strictly.
 */
public class EnsureAuthzenResponseBodyMatchesIdempotencyCheck extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response", strings = "authzen_idempotency_first_response_body")
	public Environment evaluate(Environment env) {
		JsonObject expected = JsonParser.parseString(env.getString("authzen_idempotency_first_response_body")).getAsJsonObject();
		JsonObject actual = (JsonObject) env.getElementFromObject("authzen_api_endpoint_response", "body_json");
		if (actual == null) {
			throw error("Current iteration has no JSON response body");
		}

		if (isSearchResponse(expected) && isSearchResponse(actual)) {
			compareSearchResponses(expected, actual);
			logSuccess("Search response matched first iteration (results compared as a multiset)", args("body", actual));
			return env;
		}

		if (!expected.equals(actual)) {
			throw error("Response body changed across consecutive identical requests — PDP is not idempotent",
				args("first_iteration_body", expected, "current_iteration_body", actual));
		}
		logSuccess("Response body matched first iteration", args("body", actual));
		return env;
	}

	private static boolean isSearchResponse(JsonObject body) {
		return body.has("results") && body.get("results").isJsonArray();
	}

	private void compareSearchResponses(JsonObject expected, JsonObject actual) {
		JsonArray expectedResults = expected.getAsJsonArray("results");
		JsonArray actualResults = actual.getAsJsonArray("results");

		if (expectedResults.size() != actualResults.size()) {
			throw error("Search results array size changed across consecutive identical requests — PDP is not idempotent",
				args("first_iteration_results", expectedResults, "current_iteration_results", actualResults));
		}
		List<JsonElement> expectedSorted = new ArrayList<>(expectedResults.asList());
		List<JsonElement> actualSorted = new ArrayList<>(actualResults.asList());
		Comparator<JsonElement> byString = Comparator.comparing(JsonElement::toString);
		expectedSorted.sort(byString);
		actualSorted.sort(byString);
		if (!expectedSorted.equals(actualSorted)) {
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

		JsonObject expectedRest = expected.deepCopy();
		expectedRest.remove("results");
		stripOpaquePageToken(expectedRest);
		JsonObject actualRest = actual.deepCopy();
		actualRest.remove("results");
		stripOpaquePageToken(actualRest);
		if (!expectedRest.equals(actualRest)) {
			throw error("Response body (excluding results and page.next_token) changed across consecutive identical requests — PDP is not idempotent",
				args("first_iteration_body", expected, "current_iteration_body", actual));
		}
	}

	private static boolean hasPageNextToken(JsonObject body) {
		return body.has("page")
			&& body.get("page").isJsonObject()
			&& body.getAsJsonObject("page").has("next_token");
	}

	private static void stripOpaquePageToken(JsonObject body) {
		if (body.has("page") && body.get("page").isJsonObject()) {
			body.getAsJsonObject("page").remove("next_token");
		}
	}
}
