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
 * SSF 1.0 8.1.2.2: a status update names the stream and the new status; a malformed request
 * is answered 400, not with an aborted test.
 */
@ExtendWith(MockitoExtension.class)
public class OIDSSFHandleStreamStatusUpdateRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFHandleStreamStatusUpdateRequest condition;

	@BeforeEach
	void setUp() {
		condition = new OIDSSFHandleStreamStatusUpdateRequest();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env.putObject("ssf", JsonParser.parseString("""
			{"streams": {"s1": {"stream_id": "s1", "_status": {"stream_id": "s1", "status": "enabled"}}}}
			""").getAsJsonObject());
	}

	private void putStatusInput(String json) {
		env.putObject("ssf", "stream_status_input", JsonParser.parseString(json).getAsJsonObject());
	}

	private JsonObject result() {
		return env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
	}

	@Test
	void updatesTheStatus() {
		putStatusInput("""
			{"stream_id": "s1", "status": "paused", "reason": "maintenance"}
			""");
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(200, OIDFJSON.getInt(result().get("status_code")));
		assertEquals("paused", OIDFJSON.getString(result().getAsJsonObject("result").get("status")));
	}

	@Test
	void answersAMissingStatusWith400() {
		putStatusInput("""
			{"stream_id": "s1", "reason": "maintenance"}
			""");
		assertThrows(ConditionError.class, () -> condition.execute(env));
		assertEquals(400, OIDFJSON.getInt(result().get("status_code")));
	}

	@Test
	void answersANonStringStatusWith400() {
		putStatusInput("""
			{"stream_id": "s1", "status": 1}
			""");
		assertThrows(ConditionError.class, () -> condition.execute(env));
		assertEquals(400, OIDFJSON.getInt(result().get("status_code")));
	}

	@Test
	void answersAnUnknownStatusValueWith400() {
		putStatusInput("""
			{"stream_id": "s1", "status": "sleeping"}
			""");
		assertThrows(ConditionError.class, () -> condition.execute(env));
		assertEquals(400, OIDFJSON.getInt(result().get("status_code")));
	}

	@Test
	void answersANonStringReasonWith400() {
		putStatusInput("""
			{"stream_id": "s1", "status": "paused", "reason": {"text": "maintenance"}}
			""");
		assertThrows(ConditionError.class, () -> condition.execute(env));
		assertEquals(400, OIDFJSON.getInt(result().get("status_code")));
	}
}
