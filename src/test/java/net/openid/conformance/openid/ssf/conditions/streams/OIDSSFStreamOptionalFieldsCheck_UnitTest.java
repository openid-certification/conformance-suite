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
public class OIDSSFStreamOptionalFieldsCheck_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFStreamOptionalFieldsCheck condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFStreamOptionalFieldsCheck();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	private void setStream(String json) {
		JsonObject ssf = new JsonObject();
		ssf.add("stream", JsonParser.parseString(json));
		env.putObject("ssf", ssf);
	}

	@Test
	void passesWithoutOptionalFields() {
		setStream("{\"stream_id\":\"s1\",\"delivery\":{\"method\":\"urn:ietf:rfc:8936\"}}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesWithWellTypedOptionalFields() {
		setStream("{\"events_supported\":[\"https://schemas.openid.net/secevent/caep/event-type/session-revoked\"],"
			+ "\"events_requested\":[],\"min_verification_interval\":60,\"inactivity_timeout\":86400,\"description\":\"test stream\"}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void failsWhenEventsSupportedIsNotAnArray() {
		setStream("{\"events_supported\":\"https://schemas.openid.net/secevent/caep/event-type/session-revoked\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsWhenEventsRequestedContainsANonString() {
		setStream("{\"events_requested\":[\"https://schemas.openid.net/secevent/caep/event-type/session-revoked\",42]}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsWhenMinVerificationIntervalIsAString() {
		setStream("{\"min_verification_interval\":\"60\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsWhenInactivityTimeoutIsNegative() {
		setStream("{\"inactivity_timeout\":-1}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsWhenMinVerificationIntervalIsFractional() {
		setStream("{\"min_verification_interval\":1.5}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsWhenDescriptionIsNotAString() {
		setStream("{\"description\":{\"en\":\"test stream\"}}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsWhenStreamIsMissing() {
		env.putObject("ssf", new JsonObject());
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
