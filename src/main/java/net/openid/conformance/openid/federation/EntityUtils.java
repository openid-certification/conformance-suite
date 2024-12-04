package net.openid.conformance.openid.federation;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EntityUtils {

	public static List<String> STANDARD_ENTITY_STATEMENT_CLAIMS = ImmutableList.of(
		"iss",
		"sub",
		"aud",
		"iat",
		"exp",
		"jwks",
		"authority_hints",
		"metadata",
		"metadata_policy",
		"constraints",
		"crit",
		"metadata_policy_crit",
		"trust_marks",
		"trust_mark_issuers",
		"trust_mark_owners",
		"source_endpoint"
	);

	public static String appendWellKnown(String entityIdentifier) {
		if (entityIdentifier.endsWith(".well-known/openid-federation")) {
			return entityIdentifier;
		}
		if (entityIdentifier.endsWith("/")) {
			return entityIdentifier + ".well-known/openid-federation";
		}
		return entityIdentifier + "/.well-known/openid-federation";
	}

	public static String stripWellKnown(String url) {
		String entityIdentifier = url;
		final String removingPartInUrl = ".well-known/openid-federation";
		if (url.endsWith(removingPartInUrl)) {
			entityIdentifier = url.substring(0, url.length() - removingPartInUrl.length());
		}
		return entityIdentifier;
	}

	public static String stripTrailingSlash(String url) {
		String entityIdentifier = url;
		final String removingPartInUrl = "/";
		if (url.endsWith(removingPartInUrl)) {
			entityIdentifier = url.substring(0, url.length() - removingPartInUrl.length());
		}
		return entityIdentifier;
	}

	public static boolean equals(String a, String b) {
		return Objects.equals(a, b) || Objects.equals(stripTrailingSlash(a), stripTrailingSlash(b));
	}

	public static List<String> diffEntityStatements(List<String> propertyNames, JsonElement a, JsonElement b) {

		if (propertyNames == null || propertyNames.isEmpty()) {
			throw new IllegalArgumentException("Property names list cannot be null or empty");
		}

		List<String> differences = new ArrayList<>();

		if (a == null || b == null || !a.isJsonObject() || !b.isJsonObject()) {
			return differences; // Return empty list if either element is null or not a JsonObject
		}

		JsonObject objA = a.getAsJsonObject();
		JsonObject objB = b.getAsJsonObject();

		for (String propertyName : propertyNames) {
			JsonElement propA = objA.get(propertyName);
			JsonElement propB = objB.get(propertyName);

			if (!compareJsonElements(propA, propB)) {
				differences.add(propertyName);
			}
		}

		return differences;
	}

	private static boolean compareJsonElements(JsonElement a, JsonElement b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (a.isJsonPrimitive() && b.isJsonPrimitive()) {
			return a.equals(b);
		}
		if (a.isJsonArray() && b.isJsonArray()) {
			return compareJsonArrays(a.getAsJsonArray(), b.getAsJsonArray());
		}
		if (a.isJsonObject() && b.isJsonObject()) {
			return compareJsonObjects(a.getAsJsonObject(), b.getAsJsonObject());
		}
		return false;
	}

	private static boolean compareJsonArrays(JsonArray a, JsonArray b) {
		if (a.size() != b.size()) {
			return false;
		}
		for (int i = 0; i < a.size(); i++) {
			if (!compareJsonElements(a.get(i), b.get(i))) {
				return false;
			}
		}
		return true;
	}

	private static boolean compareJsonObjects(JsonObject a, JsonObject b) {
		if (a.size() != b.size()) {
			return false;
		}
		for (Map.Entry<String, JsonElement> entry : a.entrySet()) {
			String key = entry.getKey();
			if (!b.has(key)) {
				return false;
			}
			if (!compareJsonElements(entry.getValue(), b.get(key))) {
				return false;
			}
		}
		return true;
	}


}
