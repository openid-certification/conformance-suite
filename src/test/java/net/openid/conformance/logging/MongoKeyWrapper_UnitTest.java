package net.openid.conformance.logging;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.testmodule.OIDFJSON;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the consolidated wrap/unwrap pair. The two halves used to live in separate classes
 * coupled by the bare {@code __wrapped_key_element_} string literal (write in
 * {@link GsonObjectToBsonDocumentConverter}, read in
 * {@link net.openid.conformance.CollapsingGsonHttpMessageConverter}); now they share a constant
 * and are exercised round-trip here.
 */
public class MongoKeyWrapper_UnitTest {

	// --- needsWrapping ---

	@Test
	public void needsWrapping_plainKey_false() {
		assertFalse(MongoKeyWrapper.needsWrapping("normal_key"));
	}

	@Test
	public void needsWrapping_dottedKey_true() {
		assertTrue(MongoKeyWrapper.needsWrapping("a.b.c"));
	}

	@Test
	public void needsWrapping_dollarKey_true() {
		assertTrue(MongoKeyWrapper.needsWrapping("$op"));
	}

	@Test
	public void needsWrapping_alreadyWrappedKey_true() {
		// Defensive: if input already has a wrapped-key sentinel (e.g. from a prior round-trip),
		// re-wrap it so unwrap doesn't accidentally consume it.
		assertTrue(MongoKeyWrapper.needsWrapping(MongoKeyWrapper.WRAPPED_KEY_PREFIX + "abcdef"));
	}

	// --- nextWrappedKey ---

	@Test
	public void nextWrappedKey_hasPrefixAndUniqueSuffix() {
		String k1 = MongoKeyWrapper.nextWrappedKey();
		String k2 = MongoKeyWrapper.nextWrappedKey();
		assertTrue(k1.startsWith(MongoKeyWrapper.WRAPPED_KEY_PREFIX));
		assertTrue(k2.startsWith(MongoKeyWrapper.WRAPPED_KEY_PREFIX));
		// Different suffix each call (random 6-char alphabetic).
		assertFalse(k1.equals(k2));
		assertEquals(MongoKeyWrapper.WRAPPED_KEY_PREFIX.length() + 6, k1.length());
	}

	// --- wrap ---

	@Test
	public void wrap_objectWithDottedKey_wrapsUnderEnvelope() {
		JsonObject obj = new JsonObject();
		obj.addProperty("a.b", "value");

		JsonObject wrapped = MongoKeyWrapper.wrap(obj).getAsJsonObject();

		assertEquals(1, wrapped.size());
		String wrappedKey = wrapped.keySet().iterator().next();
		assertTrue(wrappedKey.startsWith(MongoKeyWrapper.WRAPPED_KEY_PREFIX));
		JsonObject envelope = wrapped.getAsJsonObject(wrappedKey);
		assertEquals("a.b", OIDFJSON.getString(envelope.get("key")));
		assertEquals("value", OIDFJSON.getString(envelope.get("value")));
	}

	@Test
	public void wrap_objectWithDollarKey_wrapsUnderEnvelope() {
		JsonObject obj = new JsonObject();
		obj.addProperty("$op", "value");

		JsonObject wrapped = MongoKeyWrapper.wrap(obj).getAsJsonObject();

		String wrappedKey = wrapped.keySet().iterator().next();
		assertEquals("$op", OIDFJSON.getString(wrapped.getAsJsonObject(wrappedKey).get("key")));
	}

	@Test
	public void wrap_plainKey_passesThrough() {
		JsonObject obj = new JsonObject();
		obj.addProperty("name", "value");

		JsonObject wrapped = MongoKeyWrapper.wrap(obj).getAsJsonObject();

		assertEquals(1, wrapped.size());
		assertTrue(wrapped.has("name"));
		assertEquals("value", OIDFJSON.getString(wrapped.get("name")));
	}

	@Test
	public void wrap_jsonNull_replacedWithSentinel() {
		JsonObject obj = new JsonObject();
		obj.add("n", JsonNull.INSTANCE);

		JsonObject wrapped = MongoKeyWrapper.wrap(obj).getAsJsonObject();

		assertEquals(MongoKeyWrapper.JSON_NULL_SENTINEL, OIDFJSON.getString(wrapped.get("n")));
	}

	@Test
	public void wrap_recursesIntoNestedObjects() {
		JsonObject root = JsonParser.parseString(
			"{\"configs\":{\"eu.europa.ec.eudi.pid.1\":{\"format\":\"vc+sd-jwt\"}}}").getAsJsonObject();

		JsonObject wrapped = MongoKeyWrapper.wrap(root).getAsJsonObject();

		JsonObject configs = wrapped.getAsJsonObject("configs");
		assertEquals(1, configs.size());
		String envKey = configs.keySet().iterator().next();
		assertTrue(envKey.startsWith(MongoKeyWrapper.WRAPPED_KEY_PREFIX));
		assertEquals("eu.europa.ec.eudi.pid.1",
			OIDFJSON.getString(configs.getAsJsonObject(envKey).get("key")));
	}

	@Test
	public void wrap_recursesIntoArrays() {
		JsonArray arr = new JsonArray();
		JsonObject item = new JsonObject();
		item.addProperty("a.b", "v");
		arr.add(item);

		JsonArray wrapped = MongoKeyWrapper.wrap(arr).getAsJsonArray();

		assertEquals(1, wrapped.size());
		JsonObject wrappedItem = wrapped.get(0).getAsJsonObject();
		String wrappedKey = wrappedItem.keySet().iterator().next();
		assertTrue(wrappedKey.startsWith(MongoKeyWrapper.WRAPPED_KEY_PREFIX));
	}

	@Test
	public void wrap_reWrapsAlreadyWrappedKey() {
		// If a prior round-trip left a __wrapped_key_element_X key in the data, wrap re-wraps
		// it so unwrap doesn't accidentally consume the user data.
		JsonObject obj = new JsonObject();
		obj.addProperty(MongoKeyWrapper.WRAPPED_KEY_PREFIX + "xyz", "user value");

		JsonObject wrapped = MongoKeyWrapper.wrap(obj).getAsJsonObject();

		String envKey = wrapped.keySet().iterator().next();
		assertTrue(envKey.startsWith(MongoKeyWrapper.WRAPPED_KEY_PREFIX));
		// The inner envelope preserves the original key.
		assertEquals(MongoKeyWrapper.WRAPPED_KEY_PREFIX + "xyz",
			OIDFJSON.getString(wrapped.getAsJsonObject(envKey).get("key")));
	}

	// --- unwrap ---

	@Test
	public void unwrap_envelopeRestoresOriginalKey() {
		// Build the envelope form directly (mirrors what Mongo stores).
		Document envelope = new Document("key", "a.b").append("value", "v");
		Document wrapped = new Document(MongoKeyWrapper.WRAPPED_KEY_PREFIX + "xyz", envelope);

		Document unwrapped = (Document) MongoKeyWrapper.unwrap(wrapped);

		assertEquals(1, unwrapped.size());
		assertEquals("v", unwrapped.get("a.b"));
	}

	@Test
	public void unwrap_stripsClassField() {
		// MappingMongoConverter writes _class for polymorphic maps; the read side must hide it.
		Document doc = new Document("normal", "value").append("_class", "java.util.HashMap");

		Document unwrapped = (Document) MongoKeyWrapper.unwrap(doc);

		assertFalse(unwrapped.containsKey("_class"));
		assertEquals("value", unwrapped.get("normal"));
	}

	@Test
	public void unwrap_sentinelString_restoresJsonNull() {
		Document doc = new Document("n", MongoKeyWrapper.JSON_NULL_SENTINEL);

		Document unwrapped = (Document) MongoKeyWrapper.unwrap(doc);

		assertInstanceOf(JsonNull.class, unwrapped.get("n"));
	}

	@Test
	public void unwrap_recursesIntoLists() {
		Document inner = new Document("key", "a.b").append("value", "v");
		Document item = new Document(MongoKeyWrapper.WRAPPED_KEY_PREFIX + "id", inner);
		List<?> list = List.of(item);

		@SuppressWarnings("unchecked")
		List<Object> unwrapped = (List<Object>) MongoKeyWrapper.unwrap(list);

		assertEquals(1, unwrapped.size());
		Document unwrappedItem = (Document) unwrapped.get(0);
		assertEquals("v", unwrappedItem.get("a.b"));
	}

	@Test
	public void unwrap_passesThroughPrimitive() {
		assertEquals("hello", MongoKeyWrapper.unwrap("hello"));
		assertEquals(42, MongoKeyWrapper.unwrap(42));
	}

	// --- round-trip: wrap → JSON string → Document.parse → unwrap → equivalent original ---

	@Test
	public void roundTrip_dottedAndDollarKeys_preservesOriginal() {
		JsonObject original = JsonParser.parseString(
			"{\"plain\":\"v1\",\"a.b\":\"v2\",\"$op\":\"v3\",\"nested\":{\"c.d\":\"v4\"}}"
		).getAsJsonObject();

		JsonElement wrapped = MongoKeyWrapper.wrap(original);
		Document stored = Document.parse(wrapped.toString());
		Document unwrapped = (Document) MongoKeyWrapper.unwrap(stored);

		assertEquals("v1", unwrapped.get("plain"));
		assertEquals("v2", unwrapped.get("a.b"));
		assertEquals("v3", unwrapped.get("$op"));
		Document nested = (Document) unwrapped.get("nested");
		assertEquals("v4", nested.get("c.d"));
	}

	@Test
	public void roundTrip_jsonNull_restoredAsJsonNull() {
		JsonObject original = new JsonObject();
		original.add("n", JsonNull.INSTANCE);

		JsonElement wrapped = MongoKeyWrapper.wrap(original);
		Document stored = Document.parse(wrapped.toString());
		Document unwrapped = (Document) MongoKeyWrapper.unwrap(stored);

		assertInstanceOf(JsonNull.class, unwrapped.get("n"));
	}
}
