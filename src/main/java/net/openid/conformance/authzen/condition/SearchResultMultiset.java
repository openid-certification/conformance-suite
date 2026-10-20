package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Compares two search result sets as multisets: order-independent but multiplicity-preserving.
 * Each result is compared without its {@code properties}, which describe the entity rather than
 * identify it (Section 5) and may carry values that change between requests; every other member
 * is compared.
 */
final class SearchResultMultiset {

	private SearchResultMultiset() {
	}

	/** Whether the two result sets hold the same entities, ignoring properties. */
	static boolean sameEntities(JsonArray a, JsonArray b) {
		return a.size() == b.size() && countOccurrences(withoutProperties(a)).equals(countOccurrences(withoutProperties(b)));
	}

	/** Whether the two result sets are equal as multisets, properties included. */
	static boolean sameResults(JsonArray a, JsonArray b) {
		return countOccurrences(a).equals(countOccurrences(b));
	}

	/**
	 * How many of {@code results} the expected entity identifies: those that have every member it
	 * gives (type and id, or an action's name) with the same value. Properties are OPTIONAL
	 * (Sections 5.1-5.3) and not compared, nor are members the expected entity does not give.
	 */
	static int countIdentifying(JsonArray results, JsonElement expectedEntity) {
		int count = 0;
		for (JsonElement result : results) {
			if (identifies(expectedEntity, result)) {
				count++;
			}
		}
		return count;
	}

	private static boolean identifies(JsonElement expectedEntity, JsonElement result) {
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

	private static JsonArray withoutProperties(JsonArray results) {
		JsonArray stripped = new JsonArray();
		for (JsonElement result : results) {
			if (result.isJsonObject() && result.getAsJsonObject().has("properties")) {
				JsonObject copy = result.getAsJsonObject().deepCopy();
				copy.remove("properties");
				stripped.add(copy);
			} else {
				stripped.add(result);
			}
		}
		return stripped;
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
}
