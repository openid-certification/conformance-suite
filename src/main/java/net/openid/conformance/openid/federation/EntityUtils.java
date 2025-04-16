package net.openid.conformance.openid.federation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class EntityUtils {

	public static MediaType ENTITY_STATEMENT_JWT = new MediaType("application", "entity-statement+jwt");
	public static MediaType RESOLVE_RESPONSE_JWT = new MediaType("application", "resolve-response+jwt");

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

	public static Set<String> STANDARD_ENTITY_TYPES = ImmutableSet.of(
		"federation_entity",
		"openid_relying_party",
		"openid_provider",
		"oauth_authorization_server",
		"oauth_client",
		"oauth_resource"
	);

	public static Set<String> STANDARD_FEDERATION_ENTITY_URL_KEYS = ImmutableSet.of(
		"federation_fetch_endpoint",
		"federation_list_endpoint",
		"federation_resolve_endpoint",
		"federation_trust_mark_status_endpoint",
		"federation_trust_mark_list_endpoint",
		"federation_trust_mark_endpoint",
		"federation_historical_keys_endpoint"
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

	public static JsonObject createBasicClaimsObject(String iss, String sub) {
		JsonObject claims = new JsonObject();
		claims.addProperty("iss", iss);
		claims.addProperty("sub", sub);

		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(5 * 60);
		claims.addProperty("iat", iat.getEpochSecond());
		claims.addProperty("exp", exp.getEpochSecond());

		return claims;
	}

}
