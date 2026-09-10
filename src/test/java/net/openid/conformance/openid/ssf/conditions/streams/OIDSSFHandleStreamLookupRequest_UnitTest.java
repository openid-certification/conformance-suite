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
public class OIDSSFHandleStreamLookupRequest_UnitTest {

	private static final String STREAM_ID = "stream_123";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFHandleStreamLookupRequest condition;

	@BeforeEach
	void setUp() {
		condition = new OIDSSFHandleStreamLookupRequest();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);

		JsonObject ssf = JsonParser.parseString("""
			{"streams": {"%s": {"stream_id": "%s", "iss": "https://transmitter.example.com", "_status": {"status": "enabled"}}}}
			""".formatted(STREAM_ID, STREAM_ID)).getAsJsonObject();
		env.putObject("ssf", ssf);
	}

	private void lookup(String streamId) {
		JsonObject queryParams = new JsonObject();
		if (streamId != null) {
			queryParams.addProperty("stream_id", streamId);
		}
		JsonObject incomingRequest = new JsonObject();
		incomingRequest.add("query_string_params", queryParams);
		env.putObject("incoming_request", incomingRequest);
	}

	private JsonObject result() {
		return env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
	}

	@Test
	void returnsTheStreamForAKnownStreamId() {
		lookup(STREAM_ID);
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(200, OIDFJSON.getInt(result().get("status_code")));
		assertEquals(STREAM_ID, OIDFJSON.getString(result().getAsJsonObject("result").get("stream_id")));
		assertFalse(result().getAsJsonObject("result").has("_status"), "internal members are not exposed");
	}

	@Test
	void answersAnUnknownStreamIdWith404WithoutGradingIt() {
		// SSF 1.0 8.1.1.2, Table 3: 404 is the transmitter's regular answer for an unknown
		// stream_id, e.g. a receiver reading the stream of an earlier run before creating one
		lookup("stream_from_an_earlier_run");
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(404, OIDFJSON.getInt(result().get("status_code")));
		assertEquals("not_found", OIDFJSON.getString(result().getAsJsonObject("error").get("err")));
		assertFalse(result().has("result"));
	}

	@Test
	void listsTheStreamsWithoutAStreamId() {
		lookup(null);
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(200, OIDFJSON.getInt(result().get("status_code")));
		assertEquals(1, result().getAsJsonArray("result").size());
		assertEquals(STREAM_ID, OIDFJSON.getString(result().get("stream_id")));
	}

	@Test
	void listsAnEmptyArrayWhenNoStreamExists() {
		// SSF 1.0 8.1.1.2: "In the event that there are no Event Streams configured, the
		// Transmitter MUST return an empty list"
		env.getElementFromObject("ssf", "streams").getAsJsonObject().remove(STREAM_ID);
		lookup(null);
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(200, OIDFJSON.getInt(result().get("status_code")));
		assertTrue(result().getAsJsonArray("result").isEmpty());
	}
}
