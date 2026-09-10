package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureUnknownRequestedEventTypeIgnored_UnitTest {

	private static final String UNKNOWN = OIDSSFPrepareStreamConfigObjectAddUnknownRequestedEvent.UNKNOWN_EVENT_TYPE;

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureUnknownRequestedEventTypeIgnored createCondition() {
		OIDSSFEnsureUnknownRequestedEventTypeIgnored condition = new OIDSSFEnsureUnknownRequestedEventTypeIgnored();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepare(String streamJson) {
		JsonObject ssf = new JsonObject();
		ssf.addProperty("unknown_requested_event_type", UNKNOWN);
		ssf.add("stream", JsonParser.parseString(streamJson));
		env.putObject("ssf", ssf);
	}

	@Test
	void prepareConditionAddsTheUnknownTypeToEventsRequested() {
		JsonObject ssf = new JsonObject();
		JsonObject stream = new JsonObject();
		stream.add("config", JsonParser.parseString("{\"events_requested\":[\"a\"]}"));
		ssf.add("stream", stream);
		env.putObject("ssf", ssf);

		OIDSSFPrepareStreamConfigObjectAddUnknownRequestedEvent prepare = new OIDSSFPrepareStreamConfigObjectAddUnknownRequestedEvent();
		prepare.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		prepare.execute(env);

		assertTrue(env.getElementFromObject("ssf", "stream.config.events_requested").getAsJsonArray().toString().contains(UNKNOWN));
		assertTrue(UNKNOWN.equals(env.getString("ssf", "unknown_requested_event_type")));
	}

	@Test
	void passesWhenUnknownTypeIsIgnored() {
		prepare("{\"events_supported\":[\"a\"],\"events_requested\":[\"a\",\"" + UNKNOWN + "\"],\"events_delivered\":[\"a\"]}");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void failsWhenUnknownTypeIsDelivered() {
		prepare("{\"events_supported\":[\"a\"],\"events_delivered\":[\"a\",\"" + UNKNOWN + "\"]}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsWhenUnknownTypeIsClaimedAsSupported() {
		prepare("{\"events_supported\":[\"a\",\"" + UNKNOWN + "\"],\"events_delivered\":[\"a\"]}");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsWithoutPreparedUnknownType() {
		JsonObject ssf = new JsonObject();
		ssf.add("stream", JsonParser.parseString("{\"events_delivered\":[]}"));
		env.putObject("ssf", ssf);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}
}
