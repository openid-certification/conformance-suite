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
public class OIDSSFValidateCaepAssuranceLevelChangeEvent_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFValidateCaepAssuranceLevelChangeEvent condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFValidateCaepAssuranceLevelChangeEvent();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	private void setUpCaepEvent(JsonObject eventData) {
		JsonObject ssf = new JsonObject();
		ssf.add("caep_event", new JsonObject());
		ssf.getAsJsonObject("caep_event").add("data", eventData);
		env.putObject("ssf", ssf);
	}

	private static JsonObject specExampleIncrease() {
		// CAEP 1.0 Figure 10
		return JsonParser.parseString("""
			{
				"namespace": "NIST-AAL",
				"current_level": "nist-aal2",
				"previous_level": "nist-aal1",
				"change_direction": "increase",
				"initiating_entity": "user",
				"event_timestamp": 1615304991
			}""").getAsJsonObject();
	}

	@Test
	void shouldPassWithSpecExampleIncrease() {
		setUpCaepEvent(specExampleIncrease());
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWithSpecExampleCustomNamespace() {
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
	void shouldPassWithDecrease() {
		JsonObject data = specExampleIncrease();
		data.addProperty("change_direction", "decrease");
		setUpCaepEvent(data);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWhenPreviousLevelPresentWithoutChangeDirection() {
		// The SHOULD is checked by OIDSSFWarnCaepAssuranceLevelChangeDirectionMissing, not here
		JsonObject data = specExampleIncrease();
		data.remove("change_direction");
		setUpCaepEvent(data);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWhenNamespaceIsMissing() {
		JsonObject data = specExampleIncrease();
		data.remove("namespace");
		setUpCaepEvent(data);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("namespace"));
	}

	@Test
	void shouldFailWhenNamespaceIsEmpty() {
		JsonObject data = specExampleIncrease();
		data.addProperty("namespace", "");
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenNamespaceIsNotString() {
		JsonObject data = specExampleIncrease();
		data.addProperty("namespace", 8176);
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenCurrentLevelIsMissing() {
		JsonObject data = specExampleIncrease();
		data.remove("current_level");
		setUpCaepEvent(data);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("current_level"));
	}

	@Test
	void shouldFailWhenCurrentLevelIsEmpty() {
		JsonObject data = specExampleIncrease();
		data.addProperty("current_level", "");
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenCurrentLevelIsNotString() {
		JsonObject data = specExampleIncrease();
		data.addProperty("current_level", 2);
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenPreviousLevelIsNotString() {
		JsonObject data = specExampleIncrease();
		data.addProperty("previous_level", 1);
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenChangeDirectionIsNotString() {
		JsonObject data = specExampleIncrease();
		data.addProperty("change_direction", true);
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenChangeDirectionIsNotIncreaseOrDecrease() {
		JsonObject data = specExampleIncrease();
		data.addProperty("change_direction", "up");
		setUpCaepEvent(data);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("change_direction"));
	}
}
