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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SSF 1.0 8.1.1.4: "Missing Receiver-Supplied properties MUST be interpreted as requested to
 * be deleted" - a PUT replaces the configuration, unlike a PATCH.
 */
@ExtendWith(MockitoExtension.class)
public class OIDSSFHandleStreamReplaceRequest_UnitTest {

	private static final String STREAM = "stream_1";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFHandleStreamReplaceRequest condition;

	@BeforeEach
	void setUp() {
		condition = new OIDSSFHandleStreamReplaceRequest();
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
						"_status": {"stream_id": "%s", "status": "paused", "reason": "maintenance"}
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
	void omittedDescriptionIsDeleted() {
		putBody("""
			{"stream_id": "%s", "events_requested": ["urn:example:event:a"], "delivery": {"method": "urn:ietf:rfc:8936"}}
			""".formatted(STREAM));
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(200, OIDFJSON.getInt(result().get("status_code")));
		assertFalse(storedStream().has("description"), "PUT without description deletes it");
		assertFalse(result().getAsJsonObject("result").has("description"));
	}

	@Test
	void omittedEventsRequestedIsDeletedAndEventsDeliveredRecomputed() {
		putBody("""
			{"stream_id": "%s", "description": "new", "delivery": {"method": "urn:ietf:rfc:8936"}}
			""".formatted(STREAM));
		assertDoesNotThrow(() -> condition.execute(env));
		assertFalse(storedStream().has("events_requested"));
		// with no constraint from the receiver the emulated transmitter delivers everything it supports
		assertEquals(3, storedStream().getAsJsonArray("events_delivered").size());
	}

	@Test
	void presentPropertiesReplaceTheOldOnes() {
		putBody("""
			{"stream_id": "%s", "description": "new", "events_requested": ["urn:example:event:c"],
			 "delivery": {"method": "urn:ietf:rfc:8936"}}
			""".formatted(STREAM));
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals("new", OIDFJSON.getString(storedStream().get("description")));
		assertEquals(1, storedStream().getAsJsonArray("events_delivered").size());
		assertEquals("urn:example:event:c", OIDFJSON.getString(storedStream().getAsJsonArray("events_delivered").get(0)));
		assertEquals("urn:ietf:rfc:8936", OIDFJSON.getString(storedStream().getAsJsonObject("delivery").get("method")));
		assertTrue(OIDFJSON.getString(storedStream().getAsJsonObject("delivery").get("endpoint_url")).contains("stream_id=" + STREAM));
	}

	@Test
	void rejectsAReplaceThatSwitchesToAnUnsupportedDeliveryMethod() {
		env.putArray("ssf", "delivery_methods_supported", OIDFJSON.convertListToJsonArray(List.of("urn:ietf:rfc:8935")));
		// omitting delivery means poll (SSF 1.0 8.1.1.1), which this push-only run does not support
		putBody("""
			{"stream_id": "%s", "events_requested": ["urn:example:event:a"]}
			""".formatted(STREAM));
		assertThrows(ConditionError.class, () -> condition.execute(env));
		assertEquals(400, OIDFJSON.getInt(result().get("status_code")));
		assertEquals("urn:ietf:rfc:8935", OIDFJSON.getString(storedStream().getAsJsonObject("delivery").get("method")), "the stored stream is untouched");
	}

	@Test
	void replacingTheConfigurationDoesNotTouchTheStreamStatus() {
		// the status is managed through the status endpoint (SSF 1.0 8.1.2), not by PUT
		putBody("""
			{"stream_id": "%s", "delivery": {"method": "urn:ietf:rfc:8936"}}
			""".formatted(STREAM));
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals("paused", OIDFJSON.getString(storedStream().getAsJsonObject("_status").get("status")));
		assertEquals("maintenance", OIDFJSON.getString(storedStream().getAsJsonObject("_status").get("reason")));
	}
}
