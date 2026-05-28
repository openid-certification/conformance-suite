package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.HashSet;
import java.util.Set;

/**
 * Asserts that the JSON body of the current Authzen API response equals the body
 * captured by {@link CaptureAuthzenResponseBodyForIdempotencyCheck} on the first
 * iteration of an idempotency test loop.
 *
 * <p>Search responses (subject/resource/action) carry a {@code results} array whose
 * order is not guaranteed across calls, so {@code results} is compared as a set
 * while the rest of the body is compared strictly.
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
			logSuccess("Search response matched first iteration (results compared as a set)", args("body", actual));
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
		Set<JsonElement> expectedSet = new HashSet<>(expectedResults.asList());
		Set<JsonElement> actualSet = new HashSet<>(actualResults.asList());
		if (!expectedSet.equals(actualSet)) {
			throw error("Search results changed across consecutive identical requests — PDP is not idempotent",
				args("first_iteration_results", expectedResults, "current_iteration_results", actualResults));
		}

		JsonObject expectedRest = expected.deepCopy();
		expectedRest.remove("results");
		JsonObject actualRest = actual.deepCopy();
		actualRest.remove("results");
		if (!expectedRest.equals(actualRest)) {
			throw error("Response body (excluding results) changed across consecutive identical requests — PDP is not idempotent",
				args("first_iteration_body", expected, "current_iteration_body", actual));
		}
	}
}
