package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SSF 1.0 8.1.1.3: a PATCH changes the properties present in the request and leaves the
 * others alone; an unknown stream_id is answered 404.
 */
@ExtendWith(MockitoExtension.class)
public class OIDSSFHandleStreamUpdateRequest_UnitTest {

	private static final String STREAM = "stream_1";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFHandleStreamUpdateRequest condition;

	@BeforeEach
	void setUp() {
		condition = new OIDSSFHandleStreamUpdateRequest();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);

		JsonObject ssf = JsonParser.parseString("""
			{
				"issuer": "https://transmitter.example.com",
				"poll_endpoint_url": "https://transmitter.example.com/events",
				"default_config": {"events_supported": ["urn:example:event:a", "urn:example:event:b", "urn:example:event:c"]},
				"streams": {
					"%s": {
						"stream_id": "%s",
						"iss": "https://transmitter.example.com",
						"aud": "https://receiver.example.com",
						"events_supported": ["urn:example:event:a", "urn:example:event:b", "urn:example:event:c"],
						"events_requested": ["urn:example:event:a", "urn:example:event:b"],
						"events_delivered": ["urn:example:event:a", "urn:example:event:b"],
						"description": "the old description",
						"delivery": {"method": "urn:ietf:rfc:8935", "endpoint_url": "https://receiver.example.com/push"},
						"_status": {"stream_id": "%s", "status": "enabled"}
					}
				}
			}
			""".formatted(STREAM, STREAM, STREAM)).getAsJsonObject();
		env.putObject("ssf", ssf);
	}

	private void putBody(String json) {
		env.putObject("ssf", "stream_input", JsonParser.parseString(json).getAsJsonObject());
	}

	private JsonObject storedStream() {
		return env.getElementFromObject("ssf", "streams." + STREAM).getAsJsonObject();
	}

	private JsonObject result() {
		return env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
	}

	@Test
	void changesOnlyThePropertiesPresentInTheRequest() {
		putBody("""
			{"stream_id": "%s", "description": "new"}
			""".formatted(STREAM));
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(200, OIDFJSON.getInt(result().get("status_code")));
		assertEquals("new", OIDFJSON.getString(storedStream().get("description")));
		assertEquals(2, storedStream().getAsJsonArray("events_requested").size(), "an omitted property is left alone");
	}

	@Test
	void acceptsEchoedTransmitterSuppliedArraysInAnotherOrder() {
		putBody("""
			{"stream_id": "%s", "description": "new",
			 "events_supported": ["urn:example:event:c", "urn:example:event:a", "urn:example:event:b"],
			 "events_delivered": ["urn:example:event:b", "urn:example:event:a"]}
			""".formatted(STREAM));
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(200, OIDFJSON.getInt(result().get("status_code")));
	}

	@Test
	void rejectsAnEchoedTransmitterSuppliedArrayWithOtherElements() {
		putBody("""
			{"stream_id": "%s", "description": "new", "events_supported": ["urn:example:event:a"]}
			""".formatted(STREAM));
		assertThrows(ConditionError.class, () -> condition.execute(env));
		assertEquals(400, OIDFJSON.getInt(result().get("status_code")));
	}

	@Test
	void answersAnUnknownStreamWith404WithoutGrading() {
		putBody("""
			{"stream_id": "stream_unknown", "description": "new"}
			""");
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(404, OIDFJSON.getInt(result().get("status_code")));
		assertEquals("not_found", OIDFJSON.getString(result().getAsJsonObject("error").get("err")));
		assertEquals("the old description", OIDFJSON.getString(storedStream().get("description")), "the existing stream is untouched");
	}

	@Test
	void rejectsARequestWithoutStreamId() {
		putBody("""
			{"description": "new"}
			""");
		assertThrows(ConditionError.class, () -> condition.execute(env));
		assertEquals(400, OIDFJSON.getInt(result().get("status_code")));
	}
}
