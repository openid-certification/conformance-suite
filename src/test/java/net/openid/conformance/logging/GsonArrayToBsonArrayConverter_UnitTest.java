package net.openid.conformance.logging;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.testmodule.OIDFJSON;
import org.bson.BsonArray;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GsonArrayToBsonArrayConverter_UnitTest {

	private final GsonArrayToBsonArrayConverter converter = new GsonArrayToBsonArrayConverter();

	// --- convert(JsonArray) ---

	@Test
	public void convert_nullSource_returnsNull() {
		assertNull(converter.convert(null));
	}

	@Test
	public void convert_emptyArray_returnsEmptyBsonArray() {
		BsonArray result = converter.convert(new JsonArray());
		assertTrue(result.isEmpty());
	}

	@Test
	public void convert_primitivesArray_preservesValues() {
		JsonArray src = new JsonArray();
		src.add("hello");
		src.add(42);
		src.add(true);

		BsonArray result = converter.convert(src);

		assertEquals(3, result.size());
		assertEquals("hello", result.get(0).asString().getValue());
		assertEquals(42, result.get(1).asNumber().intValue());
		assertTrue(result.get(2).asBoolean().getValue());
	}

	@Test
	public void convert_arrayWithJsonNull_replacesWithNullSentinel() {
		JsonArray src = new JsonArray();
		src.add(com.google.gson.JsonNull.INSTANCE);

		BsonArray result = converter.convert(src);

		// Per GsonObjectToBsonDocumentConverter.convertFieldsToStructure, JsonNull is
		// replaced with the CONFORMANCE_SUITE_JSON_NULL constant string.
		assertEquals("CONFORMANCE_SUITE_JSON_NULL", result.get(0).asString().getValue());
	}

	@Test
	public void convert_arrayWithObjectContainingDottedKey_wrapsKey() {
		JsonArray src = new JsonArray();
		JsonObject nested = new JsonObject();
		nested.addProperty("key.with.dots", "value");
		src.add(nested);

		BsonArray result = converter.convert(src);

		// The dotted key gets wrapped under a "__wrapped_key_element_*" property so
		// that MongoDB doesn't treat it as a path expression.
		assertEquals(1, result.size());
		assertTrue(result.get(0).isDocument());
		assertTrue(result.get(0).asDocument().keySet().stream()
			.anyMatch(k -> k.startsWith("__wrapped_key_element_")));
	}

	// --- convertUnloggableValuesInMap(Map) ---

	@Test
	public void convertUnloggableValuesInMap_null_returnsNull() {
		assertNull(GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(null));
	}

	@Test
	public void convertUnloggableValuesInMap_emptyMap_returnsEmptyMap() {
		assertTrue(GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(new HashMap<>()).isEmpty());
	}

	@Test
	public void convertUnloggableValuesInMap_passesThroughStringsAndStandardNumbers() {
		Map<String, Object> in = new HashMap<>();
		in.put("s", "text");
		in.put("i", 1);
		in.put("l", 2L);
		in.put("d", 3.5);
		in.put("b", true);

		Map<String, Object> out = GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(in);

		assertEquals("text", out.get("s"));
		assertEquals(1, out.get("i"));
		assertEquals(2L, out.get("l"));
		assertEquals(3.5, out.get("d"));
		assertEquals(true, out.get("b"));
	}

	@Test
	public void convertUnloggableValuesInMap_doesNotMutateInput() {
		Map<String, Object> in = new HashMap<>();
		in.put("s", "text");

		Map<String, Object> out = GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(in);

		assertTrue(in != out);
		assertEquals(1, in.size());
	}

	@Test
	public void convertUnloggableValuesInMap_jsonArrayJsonElement_isConvertedToBsonArray() {
		JsonArray arr = new JsonArray();
		arr.add("a");
		arr.add(1);

		Map<String, Object> in = new HashMap<>();
		in.put("arr", arr);

		Map<String, Object> out = GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(in);

		assertInstanceOf(BsonArray.class, out.get("arr"));
		BsonArray converted = (BsonArray) out.get("arr");
		assertEquals(2, converted.size());
		assertEquals("a", converted.get(0).asString().getValue());
		assertEquals(1, converted.get(1).asNumber().intValue());
	}

	@Test
	public void convertUnloggableValuesInMap_nonArrayJsonElement_isLeftAsIs() {
		// convertValue only recurses on JsonArray elements — other JsonElement shapes pass
		// through unchanged (they ultimately become strings via Document.toString later).
		JsonObject obj = new JsonObject();
		obj.addProperty("x", "y");

		Map<String, Object> in = new HashMap<>();
		in.put("obj", obj);

		Map<String, Object> out = GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(in);

		assertSame(obj, out.get("obj"));
	}

	@Test
	public void convertUnloggableValuesInMap_jwk_convertsToJsonElement() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256)
			.keyUse(KeyUse.SIGNATURE)
			.keyID("unit-test")
			.generate();

		Map<String, Object> in = new HashMap<>();
		in.put("key", key);

		Map<String, Object> out = GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(in);

		assertInstanceOf(JsonElement.class, out.get("key"));
		JsonElement converted = (JsonElement) out.get("key");
		assertEquals("EC", OIDFJSON.getString(converted.getAsJsonObject().get("kty")));
		assertEquals("unit-test", OIDFJSON.getString(converted.getAsJsonObject().get("kid")));
	}

	@Test
	public void convertUnloggableValuesInMap_jwkSet_convertsToJsonElement() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).keyID("k1").generate();
		JWKSet set = new JWKSet(key);

		Map<String, Object> in = new HashMap<>();
		in.put("set", set);

		Map<String, Object> out = GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(in);

		assertInstanceOf(JsonElement.class, out.get("set"));
		JsonElement converted = (JsonElement) out.get("set");
		assertEquals(1, converted.getAsJsonObject().getAsJsonArray("keys").size());
	}

	@Test
	public void convertUnloggableValuesInMap_jwtClaimsSet_convertsToJsonElement() {
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.issuer("iss-value")
			.subject("sub-value")
			.claim("custom", 7)
			.build();

		Map<String, Object> in = new HashMap<>();
		in.put("claims", claims);

		Map<String, Object> out = GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(in);

		assertInstanceOf(JsonElement.class, out.get("claims"));
		JsonObject converted = ((JsonElement) out.get("claims")).getAsJsonObject();
		assertEquals("iss-value", OIDFJSON.getString(converted.get("iss")));
		assertEquals("sub-value", OIDFJSON.getString(converted.get("sub")));
		assertEquals(7, OIDFJSON.getInt(converted.get("custom")));
	}

	@Test
	public void convertUnloggableValuesInMap_jwsHeader_convertsToJsonElement() {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("hdr-kid").build();

		Map<String, Object> in = new HashMap<>();
		in.put("hdr", header);

		Map<String, Object> out = GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(in);

		assertInstanceOf(JsonElement.class, out.get("hdr"));
		JsonObject converted = ((JsonElement) out.get("hdr")).getAsJsonObject();
		assertEquals("ES256", OIDFJSON.getString(converted.get("alg")));
		assertEquals("hdr-kid", OIDFJSON.getString(converted.get("kid")));
	}

	// --- LazilyParsedNumber branch (the fix) ---

	@Test
	public void convertUnloggableValuesInMap_lazilyParsedIntegerNumber_becomesLong() {
		Number lazy = OIDFJSON.getNumber(JsonParser.parseString("42"));
		assertInstanceOf(com.google.gson.internal.LazilyParsedNumber.class, lazy,
			"Precondition: Gson must still return LazilyParsedNumber for integer JSON primitives");

		Map<String, Object> in = new HashMap<>();
		in.put("n", lazy);

		Map<String, Object> out = GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(in);

		assertInstanceOf(Long.class, out.get("n"));
		assertEquals(42L, out.get("n"));
	}

	@Test
	public void convertUnloggableValuesInMap_lazilyParsedLargeIntegerNumber_becomesLong() {
		// Value larger than Integer.MAX_VALUE to make sure we don't silently truncate to int.
		Number lazy = OIDFJSON.getNumber(JsonParser.parseString("4294967296"));

		Map<String, Object> in = new HashMap<>();
		in.put("n", lazy);

		Map<String, Object> out = GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(in);

		assertInstanceOf(Long.class, out.get("n"));
		assertEquals(4294967296L, out.get("n"));
	}

	@Test
	public void convertUnloggableValuesInMap_lazilyParsedFractionalNumber_becomesDouble() {
		Number lazy = OIDFJSON.getNumber(JsonParser.parseString("1.5"));

		Map<String, Object> in = new HashMap<>();
		in.put("n", lazy);

		Map<String, Object> out = GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(in);

		assertInstanceOf(Double.class, out.get("n"));
		assertEquals(1.5d, out.get("n"));
	}

	@Test
	public void convertUnloggableValuesInMap_lazilyParsedExponentNumber_becomesDouble() {
		// Long.parseLong rejects "1e10" (valid JSON number) so the fallback branch must fire.
		Number lazy = OIDFJSON.getNumber(JsonParser.parseString("1e10"));

		Map<String, Object> in = new HashMap<>();
		in.put("n", lazy);

		Map<String, Object> out = GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(in);

		assertInstanceOf(Double.class, out.get("n"));
		assertEquals(1e10d, (double) out.get("n"));
	}

	// --- List<?> branch ---

	@Test
	public void convertUnloggableValuesInMap_listOfStrings_becomesJsonArrayOfStrings() {
		Map<String, Object> in = new HashMap<>();
		in.put("list", Arrays.asList("one", "two"));

		Map<String, Object> out = GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(in);

		assertInstanceOf(JsonArray.class, out.get("list"));
		JsonArray arr = (JsonArray) out.get("list");
		assertEquals(2, arr.size());
		// Non-JsonElement items are stringified per the implementation
		assertEquals("one", OIDFJSON.getString(arr.get(0)));
		assertEquals("two", OIDFJSON.getString(arr.get(1)));
	}

	@Test
	public void convertUnloggableValuesInMap_listOfJsonPrimitives_passesPrimitivesThrough() {
		List<JsonElement> items = Arrays.asList(new JsonPrimitive("a"), new JsonPrimitive(1));

		Map<String, Object> in = new HashMap<>();
		in.put("list", items);

		Map<String, Object> out = GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(in);

		JsonArray arr = (JsonArray) out.get("list");
		assertEquals(2, arr.size());
		assertEquals("a", OIDFJSON.getString(arr.get(0)));
		assertEquals(1, OIDFJSON.getInt(arr.get(1)));
	}

	@Test
	public void convertUnloggableValuesInMap_listWithNullItem_dropsNullItem() {
		// convertValue returns null only when the input is null (it is never otherwise).
		// convert() for a List guards "else if (converted != null)" so null items are dropped.
		List<Object> items = Arrays.asList("keep", null);

		Map<String, Object> in = new HashMap<>();
		in.put("list", items);

		Map<String, Object> out = GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(in);

		JsonArray arr = (JsonArray) out.get("list");
		assertEquals(1, arr.size());
		assertEquals("keep", OIDFJSON.getString(arr.get(0)));
	}
}
