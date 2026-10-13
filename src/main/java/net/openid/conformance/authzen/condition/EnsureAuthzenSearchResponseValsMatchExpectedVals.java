package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class EnsureAuthzenSearchResponseValsMatchExpectedVals extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_search_endpoint_expected_response", "authzen_search_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonArray expected = readResultsArray(env, "authzen_search_endpoint_expected_response", "expected");
		JsonArray results = readResultsArray(env, "authzen_search_endpoint_response", "actual");

		// An empty expected array means no entity matches the search, so any result is wrong.
		if (expected.isEmpty()) {
			if (!results.isEmpty()) {
				throw error("Search result is not empty, but no entity matches this search", args("results", results));
			}
			logSuccess("The search response has an empty results array, as expected");
			return env;
		}

		// The certification scenario validates that the expected entities appear in the results but
		// does not reject additional results, so expected must be a subset of actual. An entity is
		// identified by the members the expected entity gives (type and id, or an action's name);
		// properties are OPTIONAL (Sections 5.1-5.3) and other members are unknown, which receivers
		// MUST ignore (Section 10.1.1), so neither takes part in the comparison.
		Set<JsonElement> missing = new LinkedHashSet<>();
		for (JsonElement expectedEntity : expected) {
			if (!containsEntity(results, expectedEntity)) {
				missing.add(expectedEntity);
			}
		}

		if (!missing.isEmpty()) {
			throw error("Search result is missing expected elements", args("missing expected elements", missing));
		}

		logSuccess("The search response contains all expected values");
		return env;
	}

	private static boolean containsEntity(JsonArray results, JsonElement expectedEntity) {
		for (JsonElement result : results) {
			if (identifiesSameEntity(expectedEntity, result)) {
				return true;
			}
		}
		return false;
	}

	private static boolean identifiesSameEntity(JsonElement expectedEntity, JsonElement result) {
		if (!expectedEntity.isJsonObject() || !result.isJsonObject()) {
			return expectedEntity.equals(result);
		}
		JsonObject resultObject = result.getAsJsonObject();
		for (Map.Entry<String, JsonElement> member : expectedEntity.getAsJsonObject().entrySet()) {
			if ("properties".equals(member.getKey())) {
				continue;
			}
			if (!member.getValue().equals(resultObject.get(member.getKey()))) {
				return false;
			}
		}
		return true;
	}

	private JsonArray readResultsArray(Environment env, String objectKey, String role) {
		JsonElement elem = env.getElementFromObject(objectKey, "results");
		if (elem == null) {
			throw error(role + " search response has no `results` array", args("env_key", objectKey));
		}
		if (!elem.isJsonArray()) {
			throw error(role + " search response `results` is not an array",
				args("env_key", objectKey, "value", elem));
		}
		return elem.getAsJsonArray();
	}
}
