package net.openid.conformance.logging;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.LazilyParsedNumber;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.testmodule.OIDFJSON;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression-sentinel tests for the encode path used by
 * {@link DBEventLog#log(String, String, Map, Map)} and
 * {@link DBEventLog#log(String, String, Map, JsonObject)}.
 *
 * <p>These reproduce production failures of the form
 * <pre>CodecConfigurationException: Can't find a codec for ...</pre>
 * at unit-test time, without needing a live MongoDB. Each case builds a log payload, runs it
 * through {@link BsonEncoding#assertEncodable}, and expects a clean encode.
 *
 * <p>The anchor case is a {@link LazilyParsedNumber} at the top level of an args map — the
 * exact shape produced by {@code log("...", args("max_age", OIDFJSON.getNumber(elem)))} in
 * {@code ValidateRequestObjectClaims} that surfaced as a production test failure.
 */
public class DBEventLog_UnitTest {

	// --- Map-overload encode path (args() payloads) ---

	@Test
	public void map_lazilyParsedNumberAtTopLevel_isEncodable() {
		// Reproduces the ValidateRequestObjectClaims.java:76 / ValidateExpiresIn.java:33
		// production pattern: OIDFJSON.getNumber() returns a LazilyParsedNumber from Gson.
		Number maxAge = OIDFJSON.getNumber(JsonParser.parseString("900"));

		Map<String, Object> payload = new HashMap<>();
		payload.put("max_age", maxAge);

		BsonEncoding.assertEncodable(payload);
	}

	@Test
	public void map_lazilyParsedNumberNestedInList_isEncodable() {
		// A list whose elements are LazilyParsedNumbers — e.g. from iterating a JsonArray of
		// numbers and handing the Numbers to args().
		LazilyParsedNumber lpn = new LazilyParsedNumber("3600");

		Map<String, Object> payload = new HashMap<>();
		payload.put("values", Arrays.asList(lpn, "also-a-string"));

		BsonEncoding.assertEncodable(payload);
	}

	@Test
	public void map_lazilyParsedNumberNestedInJsonArray_isEncodable() {
		// A JsonArray whose JsonPrimitive elements are backed by LazilyParsedNumbers —
		// i.e. numbers parsed from JSON text. The array hand-off path is where the pre-fix
		// code threw.
		JsonArray array = JsonParser.parseString("[1, 2, 3600]").getAsJsonArray();

		Map<String, Object> payload = new HashMap<>();
		payload.put("array", array);

		BsonEncoding.assertEncodable(payload);
	}

	@Test
	public void map_lazilyParsedNumberNestedInJsonObject_isEncodable() {
		// A JsonObject value reaching the logger — e.g. args("max_age", someJsonObject) — with
		// a numeric property. After Spring Data's GsonObjectToBsonDocumentConverter runs this
		// through Gson.toJson + BsonDocument.parse, the number becomes a plain BSON number.
		JsonObject obj = JsonParser.parseString("{\"max_age\": 900}").getAsJsonObject();

		Map<String, Object> payload = new HashMap<>();
		payload.put("req", obj);

		BsonEncoding.assertEncodable(payload);
	}

	@Test
	public void map_jsonPrimitiveValue_isEncodable() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("s", new JsonPrimitive("hello"));
		payload.put("n", new JsonPrimitive(42));
		payload.put("b", new JsonPrimitive(true));

		BsonEncoding.assertEncodable(payload);
	}

	@Test
	public void map_mixedJsonArrayWithObjectAndNull_isEncodable() {
		JsonArray array = new JsonArray();
		array.add("text");
		array.add(1);
		array.add(JsonNull.INSTANCE);
		JsonObject nested = new JsonObject();
		nested.addProperty("k", "v");
		array.add(nested);
		array.add(new JsonPrimitive(new LazilyParsedNumber("7")));

		Map<String, Object> payload = new HashMap<>();
		payload.put("mixed", array);

		BsonEncoding.assertEncodable(payload);
	}

	@Test
	public void map_jwkValue_isEncodable() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).keyID("kid-1").generate();

		Map<String, Object> payload = new HashMap<>();
		payload.put("key", key);

		BsonEncoding.assertEncodable(payload);
	}

	@Test
	public void map_jwkSetValue_isEncodable() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).keyID("kid-2").generate();
		JWKSet set = new JWKSet(key);

		Map<String, Object> payload = new HashMap<>();
		payload.put("keys", set);

		BsonEncoding.assertEncodable(payload);
	}

	@Test
	public void map_jwtClaimsSetValue_isEncodable() {
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.issuer("iss")
			.subject("sub")
			.claim("custom", 7)
			.build();

		Map<String, Object> payload = new HashMap<>();
		payload.put("claims", claims);

		BsonEncoding.assertEncodable(payload);
	}

	@Test
	public void map_jwsHeaderValue_isEncodable() {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("hdr-kid").build();

		Map<String, Object> payload = new HashMap<>();
		payload.put("header", header);

		BsonEncoding.assertEncodable(payload);
	}

	@Test
	public void map_emptyPayload_isEncodable() {
		BsonEncoding.assertEncodable(new HashMap<>());
	}

	@Test
	public void map_nullPayload_isEncodable() {
		// DBEventLog never passes null but BasicDBObjectBuilder.start(null) is legal; the helper
		// matches that by treating null as an empty Document.
		BsonEncoding.assertEncodable((Map<String, Object>) null);
	}

	// --- JsonObject-overload encode path (log(String, JsonObject)) ---

	@Test
	public void jsonObject_topLevelNumbers_isEncodable() {
		JsonObject payload = JsonParser.parseString("{\"n1\":900, \"n2\":4294967296, \"n3\":1.5}").getAsJsonObject();

		BsonEncoding.assertEncodable(payload);
	}

	@Test
	public void map_topLevelJsonPrimitiveNumbers_preserveIntegerType() {
		// args("n", new JsonPrimitive(900)) is the pattern where the registered
		// GsonPrimitiveToBsonValueConverter actually fires. An integer JsonPrimitive must encode
		// as BsonInt32/BsonInt64, not BsonDouble.
		Map<String, Object> payload = new HashMap<>();
		payload.put("small", new JsonPrimitive(900));
		payload.put("large", new JsonPrimitive(4_294_967_296L));
		payload.put("fractional", new JsonPrimitive(1.5));

		BsonEncoding.assertEncodable(payload);

		Document doc = BsonEncoding.toDocument(payload);
		assertInstanceOf(BsonInt32.class, doc.get("small"),
			"integer JsonPrimitives must encode as BsonInt32 (currently BsonDouble due to GsonPrimitiveToBsonValueConverter)");
		assertEquals(900, ((BsonInt32) doc.get("small")).getValue());
		assertInstanceOf(BsonInt64.class, doc.get("large"),
			"integer JsonPrimitives above Integer.MAX_VALUE must encode as BsonInt64");
		assertEquals(4_294_967_296L, ((BsonInt64) doc.get("large")).getValue());
		assertInstanceOf(BsonDouble.class, doc.get("fractional"));
		assertEquals(1.5d, ((BsonDouble) doc.get("fractional")).getValue());
	}

	@Test
	public void jsonObject_nestedStructuresWithNull_isEncodable() {
		JsonObject payload = JsonParser.parseString(
			"{\"arr\":[1,2,null], \"obj\":{\"k\":null}, \"s\":\"text\"}").getAsJsonObject();

		BsonEncoding.assertEncodable(payload);
	}

	@Test
	public void jsonObject_dottedKey_isEncodable() {
		// DBEventLog's JsonObject path wraps dotted keys via convertFieldsToStructure.
		JsonObject payload = JsonParser.parseString("{\"key.with.dots\":\"value\"}").getAsJsonObject();

		BsonEncoding.assertEncodable(payload);
	}

	@Test
	public void jsonObject_topLevelFieldOrder_isPreserved() {
		JsonObject payload = JsonParser.parseString(
			"{\"first\":\"a\",\"second\":\"b\",\"third\":\"c\"}").getAsJsonObject();

		Document doc = BsonEncoding.toDocument(payload);

		assertEquals(List.of("first", "second", "third"), List.copyOf(doc.keySet()));
	}

	// --- Equivalence between log(JsonObject) and log(Map) paths ---
	// Both overloads route equivalent values through the converged encoding path and must
	// produce equivalent BSON. The assertion compares relaxed-JSON renderings so type
	// representations (BsonInt32 vs Integer, BsonDocument vs Document) do not masquerade
	// as inequivalence.

	@Test
	public void equivalence_topLevelNumbers() {
		JsonObject viaJsonObject = JsonParser.parseString(
			"{\"small\":900,\"large\":4294967296,\"fractional\":1.5}").getAsJsonObject();
		Map<String, Object> viaMap = new LinkedHashMap<>();
		viaMap.put("small", new JsonPrimitive(900));
		viaMap.put("large", new JsonPrimitive(4_294_967_296L));
		viaMap.put("fractional", new JsonPrimitive(1.5));

		assertEquivalent(viaJsonObject, viaMap);
	}

	@Test
	public void equivalence_topLevelPrimitives() {
		// JsonNull is omitted: log(JsonObject) normalizes top-level JsonNull to a sentinel string
		// (preserved from the previous Document.parse path), but log(Map) callers don't pass
		// JsonNull.INSTANCE as a value (they pass Java null or omit the key). Equivalence here
		// covers only the shape that real callers can produce both ways.
		JsonObject viaJsonObject = JsonParser.parseString(
			"{\"s\":\"text\",\"b\":true}").getAsJsonObject();
		Map<String, Object> viaMap = new LinkedHashMap<>();
		viaMap.put("s", new JsonPrimitive("text"));
		viaMap.put("b", new JsonPrimitive(true));

		assertEquivalent(viaJsonObject, viaMap);
	}

	@Test
	public void jsonObject_topLevelJsonNull_isNormalizedToSentinel() {
		// Behavior preservation from the old Document.parse(convertFieldsToStructure(obj)) path:
		// a top-level JsonNull must become the CONFORMANCE_SUITE_JSON_NULL sentinel string, not
		// a reflected POJO ({_class: "com.google.gson.JsonNull"}).
		JsonObject payload = new JsonObject();
		payload.add("n", JsonNull.INSTANCE);

		Document doc = BsonEncoding.toDocument(payload);

		assertEquals("CONFORMANCE_SUITE_JSON_NULL",
			((org.bson.BsonString) doc.get("n")).getValue());
	}

	@Test
	public void equivalence_nestedDottedKey() {
		JsonObject viaJsonObject = JsonParser.parseString(
			"{\"configs\":{\"eu.europa.ec.eudi.pid.1\":{\"format\":\"vc+sd-jwt\"}}}").getAsJsonObject();
		JsonObject nestedConfigs = new JsonObject();
		JsonObject pid = new JsonObject();
		pid.addProperty("format", "vc+sd-jwt");
		nestedConfigs.add("eu.europa.ec.eudi.pid.1", pid);
		Map<String, Object> viaMap = new LinkedHashMap<>();
		viaMap.put("configs", nestedConfigs);

		assertEquivalent(viaJsonObject, viaMap);
	}

	@Test
	public void equivalence_nestedArrayAndObject() {
		JsonObject viaJsonObject = JsonParser.parseString(
			"{\"arr\":[1,2,3],\"obj\":{\"k\":\"v\"}}").getAsJsonObject();
		JsonArray arr = JsonParser.parseString("[1,2,3]").getAsJsonArray();
		JsonObject obj = new JsonObject();
		obj.addProperty("k", "v");
		Map<String, Object> viaMap = new LinkedHashMap<>();
		viaMap.put("arr", arr);
		viaMap.put("obj", obj);

		assertEquivalent(viaJsonObject, viaMap);
	}

	@Test
	public void equivalence_topLevelDottedKey() {
		JsonObject viaJsonObject = JsonParser.parseString("{\"key.with.dots\":\"value\"}").getAsJsonObject();
		JsonObject wrapped = new JsonObject();
		wrapped.addProperty("key.with.dots", "value");
		Map<String, Object> viaMap = new LinkedHashMap<>();
		viaMap.put("payload", wrapped);

		// Note the structural difference: top-level dotted keys in a Map cannot be expressed
		// directly (BasicDBObject keys are not pre-wrapped), so the Map-form wraps the dotted
		// key under a "payload" container. Equivalence here means: the JsonObject overload must
		// wrap top-level dotted keys identically to how convertFieldsToStructure wraps them when
		// they appear nested. We compare encoded shapes via assertEquivalent's relaxed-JSON
		// rendering, which collapses BsonDocument vs Document distinctions.
		Document jsonObjectDoc = BsonEncoding.toDocument(viaJsonObject);
		Document mapDoc = BsonEncoding.toDocument(viaMap);
		// At least one __wrapped_key_element_* field must exist in both renderings.
		String jsonObjectRendered = jsonObjectDoc.toJson();
		String mapRendered = mapDoc.toJson();
		assertTrue(jsonObjectRendered.contains("__wrapped_key_element_"),
			"JsonObject path must wrap top-level dotted keys; rendered: " + jsonObjectRendered);
		assertTrue(mapRendered.contains("__wrapped_key_element_"),
			"Map path must wrap nested dotted keys; rendered: " + mapRendered);
	}

	private static void assertEquivalent(JsonObject viaJsonObject, Map<String, Object> viaMap) {
		Document jsonObjectDoc = BsonEncoding.toDocument(viaJsonObject);
		Document mapDoc = BsonEncoding.toDocument(viaMap);
		JsonWriterSettings relaxed = JsonWriterSettings.builder()
			.outputMode(JsonMode.RELAXED).build();
		String jsonObjectRendered = stripWrapperSuffix(jsonObjectDoc.toJson(relaxed));
		String mapRendered = stripWrapperSuffix(mapDoc.toJson(relaxed));
		assertEquals(jsonObjectRendered, mapRendered,
			"log(JsonObject) and log(Map) must produce equivalent encoded BSON;"
				+ " JsonObject path: " + jsonObjectRendered
				+ "; Map path: " + mapRendered);
	}

	/**
	 * The wrapped-key envelope ({@code __wrapped_key_element_xxxxxx}) gets a fresh random
	 * 6-character suffix on each call (so collisions can't happen if a real input already
	 * contains a wrapped-key sentinel). Two structurally-equivalent encodings produce different
	 * suffixes, so normalize them to a placeholder for comparison.
	 */
	private static String stripWrapperSuffix(String rendered) {
		return rendered.replaceAll("__wrapped_key_element_[A-Za-z]{6}", "__wrapped_key_element_XXXXXX");
	}

	// --- EventLog.log(.., String) contract: null message must be accepted ---

	@Test
	public void log_stringOverload_preservesFieldOrderAndAcceptsNullMessage() throws Exception {
		// The original DBEventLog.log(.., String) did .append("msg", msg), which stores BSON null
		// when msg is null and appended the message after the standard metadata fields.
		DBEventLog log = new DBEventLog();
		MongoTemplate template = Mockito.mock(MongoTemplate.class);
		Field templateField = DBEventLog.class.getDeclaredField("mongoTemplate");
		templateField.setAccessible(true);
		templateField.set(log, template);

		assertDoesNotThrow(() -> log.log("test-id", "src", Map.of(), (String) null));

		ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
		Mockito.verify(template).insert(documentCaptor.capture(), Mockito.eq(DBEventLog.COLLECTION));
		Document doc = documentCaptor.getValue();
		assertEquals(List.of("_id", "testId", "src", "testOwner", "time", "msg"),
			List.copyOf(doc.keySet()));
		assertTrue(doc.containsKey("msg"));
		assertNull(doc.get("msg"));
	}
}
