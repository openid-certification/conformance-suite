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

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureEventsDeliveredIsSubsetOfSupportedAndRequested_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureEventsDeliveredIsSubsetOfSupportedAndRequested createCondition() {
		OIDSSFEnsureEventsDeliveredIsSubsetOfSupportedAndRequested condition = new OIDSSFEnsureEventsDeliveredIsSubsetOfSupportedAndRequested();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepareStream(String streamJson) {
		JsonObject ssf = new JsonObject();
		ssf.add("stream", JsonParser.parseString(streamJson));
		env.putObject("ssf", ssf);
	}

	@Test
	void passesWhenDeliveredIsIntersectionSubset() {
		prepareStream("""
			{"events_supported":["a","b","c"],"events_requested":["a","b"],"events_delivered":["a"]}
			""");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void passesWhenDeliveredEqualsIntersection() {
		prepareStream("""
			{"events_supported":["a","b"],"events_requested":["b","a"],"events_delivered":["a","b"]}
			""");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void failsWhenDeliveredContainsUnsupportedEvent() {
		prepareStream("""
			{"events_supported":["a"],"events_requested":["a","x"],"events_delivered":["a","x"]}
			""");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsWhenDeliveredContainsUnrequestedEvent() {
		prepareStream("""
			{"events_supported":["a","b"],"events_requested":["a"],"events_delivered":["a","b"]}
			""");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void passesWithoutEventsRequestedComparingOnlySupported() {
		prepareStream("""
			{"events_supported":["a","b"],"events_delivered":["b"]}
			""");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void comparesAgainstTheSentRequestWhenAvailable() {
		// the transmitter rewrote the echoed events_requested to match events_delivered
		JsonObject ssf = new JsonObject();
		ssf.add("stream", JsonParser.parseString("""
			{"events_supported":["a","b"],"events_requested":["a","b"],"events_delivered":["a","b"]}
			"""));
		ssf.add("expected_stream_config", JsonParser.parseString("{\"events_requested\":[\"a\"]}"));
		env.putObject("ssf", ssf);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void passesWhenDeliveredMatchesTheSentRequest() {
		JsonObject ssf = new JsonObject();
		ssf.add("stream", JsonParser.parseString("""
			{"events_supported":["a","b"],"events_requested":["a","x"],"events_delivered":["a"]}
			"""));
		ssf.add("expected_stream_config", JsonParser.parseString("{\"events_requested\":[\"a\",\"x\"]}"));
		env.putObject("ssf", ssf);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void passesWithoutEventsDelivered() {
		prepareStream("{\"events_supported\":[\"a\"]}");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}
}
