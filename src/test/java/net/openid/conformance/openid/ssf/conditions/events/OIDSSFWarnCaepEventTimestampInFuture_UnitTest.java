package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFWarnCaepEventTimestampInFuture_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFWarnCaepEventTimestampInFuture condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFWarnCaepEventTimestampInFuture();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
	}

	private void setUpCaepEvent(JsonObject eventData) {
		JsonObject ssf = new JsonObject();
		ssf.add("caep_event", new JsonObject());
		ssf.getAsJsonObject("caep_event").add("data", eventData);
		ssf.getAsJsonObject("caep_event").addProperty("type", SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE);
		env.putObject("ssf", ssf);
	}

	private JsonObject dataWithTimestamp(long epochSeconds) {
		JsonObject data = new JsonObject();
		data.addProperty("event_timestamp", epochSeconds);
		return data;
	}

	@Test
	void passesWithoutTimestamp() {
		setUpCaepEvent(new JsonObject());
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesForNonNumericTimestamp() {
		JsonObject data = new JsonObject();
		data.addProperty("event_timestamp", "yesterday");
		setUpCaepEvent(data);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesForPastTimestamp() {
		setUpCaepEvent(dataWithTimestamp(Instant.now().getEpochSecond() - 3600));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesForTimestampWithinAllowedSkew() {
		setUpCaepEvent(dataWithTimestamp(Instant.now().getEpochSecond() + 60));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void failsForTimestampBeyondAllowedSkew() {
		setUpCaepEvent(dataWithTimestamp(Instant.now().getEpochSecond() + 3600));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsForMillisecondTimestamp() {
		setUpCaepEvent(dataWithTimestamp(Instant.now().toEpochMilli()));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
