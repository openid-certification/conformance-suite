package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureNoSetPushedWhileStreamPaused_UnitTest {

	private static final Instant PAUSED_AT = Instant.parse("2026-09-10T10:00:00Z");

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureNoSetPushedWhileStreamPaused condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFEnsureNoSetPushedWhileStreamPaused();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void prepare(String pausedAt, String receivedAt, boolean withPushRequest) {
		JsonObject ssf = new JsonObject();
		if (pausedAt != null) {
			ssf.addProperty("stream_paused_at", pausedAt);
		}
		if (receivedAt != null) {
			ssf.addProperty("push_request_received_at", receivedAt);
		}
		if (withPushRequest) {
			JsonObject pushRequest = new JsonObject();
			pushRequest.addProperty("body", "eyJhbGciOiJub25lIn0.e30.");
			ssf.add("push_request", pushRequest);
		}
		env.putObject("ssf", ssf);
	}

	@Test
	public void shouldPassWhenNoPushRequestWasReceived() {
		prepare(PAUSED_AT.toString(), null, false);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void shouldPassWhenPushWasReceivedBeforeThePause() {
		prepare(PAUSED_AT.toString(), PAUSED_AT.minusSeconds(2).toString(), true);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void shouldFailWhenPushWasReceivedAfterThePause() {
		prepare(PAUSED_AT.toString(), PAUSED_AT.plusSeconds(3).toString(), true);
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("while the stream status was 'paused'"));
	}

	@Test
	public void shouldFailWhenPushWasReceivedAtTheInstantOfThePause() {
		prepare(PAUSED_AT.toString(), PAUSED_AT.toString(), true);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void shouldFailWhenPausedTimestampIsMissing() {
		prepare(null, PAUSED_AT.toString(), true);
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("Missing timestamp"));
	}

	@Test
	public void shouldFailWhenReceivedTimestampIsInvalid() {
		prepare(PAUSED_AT.toString(), "yesterday", true);
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("Invalid timestamp"));
	}
}
