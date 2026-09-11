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

@ExtendWith(MockitoExtension.class)
public class OIDSSFWarnCaepRiskLevelChangeRiskReasonMissing_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFWarnCaepRiskLevelChangeRiskReasonMissing createCondition() {
		OIDSSFWarnCaepRiskLevelChangeRiskReasonMissing condition = new OIDSSFWarnCaepRiskLevelChangeRiskReasonMissing();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
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
	void passesWhenRiskReasonIsPresent() {
		prepareEventData("""
			{"current_level":"LOW","previous_level":"HIGH","principal":"USER","risk_reason":"PASSWORD_FOUND_IN_DATA_BREACH"}""");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void warnsWhenRiskReasonIsMissing() {
		prepareEventData("""
			{"current_level":"LOW","previous_level":"HIGH","principal":"USER"}""");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}
}
