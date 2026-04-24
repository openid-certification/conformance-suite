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
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
}
