package net.openid.conformance.openid.ssf.conditions.events;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFWarnCaepAssuranceLevelChangeDirectionMissing_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFWarnCaepAssuranceLevelChangeDirectionMissing condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFWarnCaepAssuranceLevelChangeDirectionMissing();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
	}

	private void setUpCaepEvent(JsonObject eventData) {
		JsonObject ssf = new JsonObject();
		ssf.add("caep_event", new JsonObject());
		ssf.getAsJsonObject("caep_event").add("data", eventData);
		env.putObject("ssf", ssf);
	}

	@Test
	void shouldPassWithSpecExampleIncrease() {
		// CAEP 1.0 Figure 10
		setUpCaepEvent(JsonParser.parseString("""
			{
				"namespace": "NIST-AAL",
				"current_level": "nist-aal2",
				"previous_level": "nist-aal1",
				"change_direction": "increase",
				"initiating_entity": "user",
				"event_timestamp": 1615304991
			}""").getAsJsonObject());
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWhenPreviousLevelIsAbsent() {
		// CAEP 1.0 Figure 11
		setUpCaepEvent(JsonParser.parseString("""
			{
				"namespace": "Retinal Scan",
				"current_level": "hi-res-scan",
				"initiating_entity": "user",
				"event_timestamp": 1615304991
			}""").getAsJsonObject());
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWhenPreviousLevelIsPresentButChangeDirectionIsMissing() {
		JsonObject data = new JsonObject();
		data.addProperty("namespace", "NIST-AAL");
		data.addProperty("current_level", "nist-aal2");
		data.addProperty("previous_level", "nist-aal1");
		setUpCaepEvent(data);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("change_direction"));
	}
}
