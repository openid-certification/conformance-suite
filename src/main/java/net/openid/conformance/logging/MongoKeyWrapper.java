package net.openid.conformance.logging;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wrap/unwrap helpers for keys MongoDB cannot store directly (those containing {@code .} or
 * {@code $}). The two halves used to live in separate classes coupled only by the bare
 * {@value #WRAPPED_KEY_PREFIX} string literal -- {@link GsonObjectToBsonDocumentConverter}
 * wrapped on write and {@code CollapsingGsonHttpMessageConverter} unwrapped on read.
 *
 * <p>The wrapper rewrites
 *
 * <pre>{"a.b": "v"}</pre>
 *
 * as
 *
 * <pre>{"__wrapped_key_element_xxxxxx": {"key": "a.b", "value": "v"}}</pre>
 *
 * with a random 6-character suffix so a key like {@value #WRAPPED_KEY_PREFIX}{@code foo}
 * already present in the user data can be wrapped again without collision. JSON nulls become
 * the {@link #JSON_NULL_SENTINEL} string on the way in and are restored on the way out.
 */
public final class MongoKeyWrapper {

	public static final String WRAPPED_KEY_PREFIX = "__wrapped_key_element_";
	public static final String JSON_NULL_SENTINEL = "CONFORMANCE_SUITE_JSON_NULL";

	private static final Logger log = LoggerFactory.getLogger(MongoKeyWrapper.class);

	private MongoKeyWrapper() {}

	/** A key needs wrapping if Mongo couldn't store it verbatim, or if it would collide with the wrapper sentinel. */
	public static boolean needsWrapping(String key) {
		return key.contains(".") || key.contains("$") || key.startsWith(WRAPPED_KEY_PREFIX);
	}

	/** Generate a fresh wrapped-key envelope id with a random suffix. */
	public static String nextWrappedKey() {
		return WRAPPED_KEY_PREFIX + RandomStringUtils.secure().nextAlphabetic(6);
	}

	/** Build the {@code {"key": k, "value": v}} envelope used inside a wrapped entry. */
	public static JsonObject buildEnvelope(String key, JsonElement value) {
		JsonObject envelope = new JsonObject();
		envelope.addProperty("key", key);
		envelope.add("value", value);
		return envelope;
	}

	/**
	 * Recursively wrap dotted, dollar, or already-wrapped keys, and replace {@link JsonNull}
	 * values with {@link #JSON_NULL_SENTINEL}. Primitive values pass through unchanged.
	 */
	public static JsonElement wrap(JsonElement source) {
		if (source.isJsonObject()) {
			JsonObject converted = new JsonObject();
			for (String key : source.getAsJsonObject().keySet()) {
				JsonElement wrappedValue = wrap(source.getAsJsonObject().get(key));
				if (needsWrapping(key)) {
					JsonObject envelope = buildEnvelope(key, wrappedValue);
					converted.add(nextWrappedKey(), envelope);
					log.info("Wrapped {} as {}", key, envelope);
				} else {
					converted.add(key, wrappedValue);
				}
			}
			return converted;
		} else if (source.isJsonArray()) {
			JsonArray converted = new JsonArray();
			for (JsonElement element : source.getAsJsonArray()) {
				converted.add(wrap(element));
			}
			return converted;
		} else if (source.isJsonNull()) {
			return new JsonPrimitive(JSON_NULL_SENTINEL);
		} else {
			return source;
		}
	}

	/**
	 * Reverse of {@link #wrap}. Recursively restores wrapped keys back to their original form,
	 * strips Spring Data Mongo's {@code _class} type marker (added on write for polymorphic
	 * maps), and turns {@link #JSON_NULL_SENTINEL} strings back into {@link JsonNull}.
	 *
	 * <p>Accepts the read-side shape used by {@code CollapsingGsonHttpMessageConverter}: a
	 * {@link Document} tree (with nested {@link Document}s and {@link List}s) as produced by
	 * the MongoDB driver's read path. Returns the same shape with envelopes collapsed.
	 */
	public static Object unwrap(Object source) {
		if (source instanceof List<?> list) {
			return list.stream().map(MongoKeyWrapper::unwrap).collect(Collectors.toCollection(ArrayList::new));
		} else if (source instanceof Document doc) {
			Document converted = new Document();
			Set<String> keys = doc.keySet();
			for (String key : keys) {
				if (key.startsWith(WRAPPED_KEY_PREFIX)) {
					Document wrapped = doc.get(key, Document.class);
					converted.put((String) wrapped.get("key"), unwrap(wrapped.get("value")));
				} else if ("_class".equals(key)) {
					// Skip Spring Data's type marker; the read side renders user data only.
				} else {
					converted.put(key, unwrap(doc.get(key)));
				}
			}
			return converted;
		} else if (JSON_NULL_SENTINEL.equals(source)) {
			return JsonNull.INSTANCE;
		} else {
			return source;
		}
	}
}
