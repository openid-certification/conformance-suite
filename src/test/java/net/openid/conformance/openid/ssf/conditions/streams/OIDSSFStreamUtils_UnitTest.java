package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils.StreamStatusValue;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OIDSSFStreamUtils_UnitTest {

	private static JsonObject stream() {
		return JsonParser.parseString("{\"stream_id\": \"s1\"}").getAsJsonObject();
	}

	@Test
	void aRequestBodyThatIsNotAnObjectYieldsNoStream() {
		Environment env = new Environment();
		JsonObject request = new JsonObject();
		request.add("body_json", JsonParser.parseString("[\"not\", \"an\", \"object\"]"));
		env.putObject("incoming_request", request);
		assertNull(OIDSSFStreamUtils.getStreamFromRequestBody(env));
	}

	@Test
	void firstUpdateInitialisesTheStatusObject() {
		JsonObject stream = stream();
		OIDSSFStreamUtils.updateStreamStatus(stream, StreamStatusValue.paused, "maintenance");
		JsonObject status = OIDSSFStreamUtils.getStreamStatus(stream);
		assertEquals("s1", OIDFJSON.getString(status.get("stream_id")));
		assertEquals("paused", OIDFJSON.getString(status.get("status")));
		assertEquals("maintenance", OIDFJSON.getString(status.get("reason")));
	}

	@Test
	void everyTransitionIsAllowedIncludingOutOfDisabled() {
		// SSF 1.0 8.1.2.2 defines no state machine; 8.1.2 lets the receiver request enable,
		// pause or disable at any time
		JsonObject stream = stream();
		OIDSSFStreamUtils.updateStreamStatus(stream, StreamStatusValue.disabled, "off");
		assertDoesNotThrow(() -> OIDSSFStreamUtils.updateStreamStatus(stream, StreamStatusValue.enabled, null));
		assertEquals(StreamStatusValue.enabled, OIDSSFStreamUtils.getStreamStatusValue(stream));
		assertNull(OIDSSFStreamUtils.getStreamStatus(stream).get("reason"), "a status update without reason clears the previous one");
		assertDoesNotThrow(() -> OIDSSFStreamUtils.updateStreamStatus(stream, StreamStatusValue.paused, null));
		assertDoesNotThrow(() -> OIDSSFStreamUtils.updateStreamStatus(stream, StreamStatusValue.disabled, null));
		assertDoesNotThrow(() -> OIDSSFStreamUtils.updateStreamStatus(stream, StreamStatusValue.paused, null));
	}

	@Test
	void onlyEnabledDeliversEvents() {
		assertEquals(true, StreamStatusValue.enabled.isEventDeliveryEnabled());
		assertFalse(StreamStatusValue.paused.isEventDeliveryEnabled());
		assertFalse(StreamStatusValue.disabled.isEventDeliveryEnabled());
	}
}
