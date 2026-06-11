package net.openid.conformance.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Small helpers for navigating gson {@link JsonObject}s outside the Environment.
 */
public final class JsonObjectUtils {

	private JsonObjectUtils() {
		// utility class
	}

	/**
	 * Walks the given keys into nested JSON objects, returning null if any step is absent or
	 * not an object.
	 */
	public static JsonElement path(JsonObject obj, String... keys) {
		JsonElement cur = obj;
		for (String key : keys) {
			if (cur == null || !cur.isJsonObject()) {
				return null;
			}
			cur = cur.getAsJsonObject().get(key);
		}
		return cur;
	}
}
