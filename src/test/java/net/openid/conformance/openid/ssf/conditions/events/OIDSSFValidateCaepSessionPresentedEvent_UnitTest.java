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
public class OIDSSFValidateCaepSessionPresentedEvent_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFValidateCaepSessionPresentedEvent condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFValidateCaepSessionPresentedEvent();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	private void setUpCaepEvent(JsonObject eventData) {
		JsonObject ssf = new JsonObject();
		ssf.add("caep_event", new JsonObject());
		ssf.getAsJsonObject("caep_event").add("data", eventData);
		env.putObject("ssf", ssf);
	}

	private static JsonObject specExample() {
		// CAEP 1.0 Section 3.7.2
		return JsonParser.parseString("""
			{
				"fp_ua": "abb0b6e7da81a42233f8f2b1a8ddb1b9a4c81611",
				"ext_id": "12345",
				"event_timestamp": 1615304991
			}""").getAsJsonObject();
	}

	@Test
	void shouldPassWithSpecExample() {
		setUpCaepEvent(specExample());
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWithNoEventSpecificClaims() {
		JsonObject data = new JsonObject();
		data.addProperty("event_timestamp", 1615304991);
		setUpCaepEvent(data);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWhenFpUaIsNotString() {
		JsonObject data = specExample();
		data.addProperty("fp_ua", 42);
		setUpCaepEvent(data);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("fp_ua"));
	}

	@Test
	void shouldFailWhenExtIdIsNotString() {
		JsonObject data = specExample();
		data.addProperty("ext_id", 12345);
		setUpCaepEvent(data);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("ext_id"));
	}
}
