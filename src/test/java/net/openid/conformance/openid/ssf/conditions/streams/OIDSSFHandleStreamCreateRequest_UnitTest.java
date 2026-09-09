package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFHandleStreamCreateRequest_UnitTest {

	private static final String AUDIENCE = "https://receiver.example.com";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFHandleStreamCreateRequest condition;

	@BeforeEach
	void setUp() {
		condition = new OIDSSFHandleStreamCreateRequest();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);

		JsonObject ssf = JsonParser.parseString("""
			{
				"issuer": "https://transmitter.example.com",
				"poll_endpoint_url": "https://transmitter.example.com/events",
				"default_config": {"events_supported": ["urn:example:event:a", "urn:example:event:b"]}
			}
			""").getAsJsonObject();
		env.putObject("ssf", ssf);
		JsonObject config = JsonParser.parseString("{\"ssf\": {\"stream\": {\"audience\": \"" + AUDIENCE + "\"}}}").getAsJsonObject();
		env.putObject("config", config);
	}

	private void streamInput(String json) {
		env.putObject("ssf", "stream_input", JsonParser.parseString(json).getAsJsonObject());
	}

	private JsonObject result() {
		return env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
	}

	@Test
	void createsTheStreamFromReceiverSuppliedValues() {
		streamInput("""
			{"events_requested": ["urn:example:event:a"], "description": "test stream"}
			""");
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(201, OIDFJSON.getInt(result().get("status_code")));
		JsonObject stream = result().getAsJsonObject("result");
		assertEquals(AUDIENCE, OIDFJSON.getString(stream.get("aud")));
		assertEquals(1, stream.getAsJsonArray("events_delivered").size());
	}

	@Test
	void honoursACreateRequestThatCarriesTransmitterSuppliedProperties() {
		// SSF 1.0 8.1.1.1 lists what a create request MAY contain; it forbids nothing, and
		// Table 1 reserves 400 for a request that "cannot be parsed". 8.1.1.1.1 even lets the
		// audience be agreed out of band, so a receiver proposing one is not an error. The
		// transmitter decides these values; the request is honoured and they are ignored.
		streamInput("""
			{"aud": "https://receiver.example.com/proposed", "iss": "https://someone.example", "stream_id": "mine",
			 "events_requested": ["urn:example:event:b"]}
			""");
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(201, OIDFJSON.getInt(result().get("status_code")));
		assertFalse(result().has("error"));
		JsonObject stream = result().getAsJsonObject("result");
		assertEquals(AUDIENCE, OIDFJSON.getString(stream.get("aud")), "the transmitter's audience wins");
		assertEquals("https://transmitter.example.com", OIDFJSON.getString(stream.get("iss")));
		assertTrue(OIDFJSON.getString(stream.get("stream_id")).startsWith("stream_"), "the transmitter assigns the stream id");
	}
}
