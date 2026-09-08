package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFValidateCaepRiskLevelChangeEvent_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFValidateCaepRiskLevelChangeEvent createCondition() {
		OIDSSFValidateCaepRiskLevelChangeEvent condition = new OIDSSFValidateCaepRiskLevelChangeEvent();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepareEventData(String json) {
		JsonObject caepEvent = new JsonObject();
		caepEvent.addProperty("type", SsfEvents.CAEP_RISK_LEVEL_CHANGE_EVENT_TYPE);
		caepEvent.add("data", JsonParser.parseString(json).getAsJsonObject());
		JsonObject ssf = new JsonObject();
		ssf.add("caep_event", caepEvent);
		env.putObject("ssf", ssf);
	}

	@Test
	void passesForSpecExamplePayload() {
		// example from CAEP 1.0 section 3.8
		prepareEventData("""
			{"current_level":"LOW","previous_level":"HIGH","event_timestamp":1615304991,
			 "principal":"USER","risk_reason":"PASSWORD_FOUND_IN_DATA_BREACH"}""");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void passesWithoutOptionalPreviousLevelAndRiskReason() {
		prepareEventData("{\"current_level\":\"MEDIUM\",\"principal\":\"SESSION\"}");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void passesForNonStandardPrincipal() {
		// CAEP 1.0 3.8: USER, DEVICE, ... "or any other entity"
		prepareEventData("{\"current_level\":\"HIGH\",\"principal\":\"WORKLOAD\"}");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void failsWhenPrincipalIsMissing() {
		prepareEventData("{\"current_level\":\"LOW\"}");
		ConditionError e = assertThrows(ConditionError.class, () -> createCondition().execute(env));
		assertTrue(e.getMessage().contains("principal"), e.getMessage());
	}

	@Test
	void failsWhenCurrentLevelIsMissing() {
		prepareEventData("{\"principal\":\"USER\"}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsForInvalidCurrentLevel() {
		prepareEventData("{\"current_level\":\"CRITICAL\",\"principal\":\"USER\"}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsForInvalidPreviousLevel() {
		prepareEventData("{\"current_level\":\"LOW\",\"previous_level\":\"UNKNOWN\",\"principal\":\"USER\"}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsForNonStringRiskReason() {
		prepareEventData("{\"current_level\":\"LOW\",\"principal\":\"USER\",\"risk_reason\":42}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}
}
