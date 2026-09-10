package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFWarnEmptyEventsRequestedInStreamRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFWarnEmptyEventsRequestedInStreamRequest condition;

	@BeforeEach
	void setUp() {
		condition = new OIDSSFWarnEmptyEventsRequestedInStreamRequest();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env.putObject("ssf", new JsonObject());
	}

	private void streamInput(String json) {
		env.putObject("ssf", "stream_input", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	void passesWhenEventsRequestedIsAbsent() {
		streamInput("{\"delivery\": {\"method\": \"urn:ietf:rfc:8936\"}}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesWhenEventsRequestedIsNotEmpty() {
		streamInput("{\"events_requested\": [\"https://schemas.openid.net/secevent/caep/event-type/session-revoked\"]}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesWhenNoStreamRequestBodyWasParsed() {
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void leavesANonArrayValueToTheRequestValidation() {
		streamInput("{\"events_requested\": \"urn:example:event:a\"}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void raisesAFindingWhenEventsRequestedIsAnEmptyArray() {
		streamInput("{\"events_requested\": []}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
