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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFCheckVerificationEventState_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFCheckVerificationEventState condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFCheckVerificationEventState();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	/**
	 * Builds an {@code ssf} environment object where {@code verification.token.claims}
	 * contains the given events object. If {@code expectedState} is non-null, it is
	 * stored under {@code verification.state} to simulate the expected state configured
	 * by the test setup.
	 */
	private void setUpVerification(JsonObject eventsObject, String expectedState) {
		JsonObject claims = new JsonObject();
		if (eventsObject != null) {
			claims.add("events", eventsObject);
		}

		JsonObject token = new JsonObject();
		token.add("claims", claims);

		JsonObject verification = new JsonObject();
		verification.add("token", token);
		if (expectedState != null) {
			verification.addProperty("state", expectedState);
		}

		JsonObject ssf = new JsonObject();
		ssf.add("verification", verification);
		env.putObject("ssf", ssf);
	}

	private static JsonObject eventsWithVerificationState(String state) {
		JsonObject verificationEvent = new JsonObject();
		if (state != null) {
			verificationEvent.addProperty("state", state);
		}
		JsonObject events = new JsonObject();
		events.add(SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE, verificationEvent);
		return events;
	}

	@Test
	void shouldPassWhenExpectedStateMatchesActualState() {
		setUpVerification(eventsWithVerificationState("abc-123"), "abc-123");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWhenNoStateExpectedAndNoStatePresent() {
		// Stream verification without state: expected state is not configured
		// and the verification event carries no state claim either.
		setUpVerification(eventsWithVerificationState(null), null);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWhenNoStateExpectedButStatePresent() {
		setUpVerification(eventsWithVerificationState("unexpected"), null);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldPassWhenStateExpectedButEventIsStateless() {
		// SSF 1.0 §8.1.4-2: "state" is optional in a verification event, so the
		// transmitter is allowed to deliver an unsolicited (or non-echoing) event
		// even when the receiver has issued a verification request with a state.
		// This covers the interop case where an unsolicited verification event
		// arrives at a receiver that has already (independently) triggered a
		// verification request.
		setUpVerification(eventsWithVerificationState(null), "abc-123");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWhenExpectedStateDoesNotMatchActualState() {
		setUpVerification(eventsWithVerificationState("actual"), "expected");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenEventsClaimIsMissing() {
		setUpVerification(null, "abc-123");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenVerificationEventTypeIsMissing() {
		// events claim is present but does not contain the verification event type
		JsonObject events = new JsonObject();
		events.add("https://schemas.openid.net/secevent/ssf/event-type/other", new JsonObject());
		setUpVerification(events, "abc-123");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
