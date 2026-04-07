package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFExtractCaepEventData_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private OIDSSFExtractCaepEventData condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFExtractCaepEventData();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void setUpSetToken(String eventType, JsonObject eventData) {
		JsonObject events = new JsonObject();
		events.add(eventType, eventData);

		JsonObject claims = new JsonObject();
		claims.add("events", events);

		JsonObject setToken = new JsonObject();
		setToken.add("claims", claims);
		env.putObject("set_token", setToken);

		// Ensure ssf object exists for caep_event storage
		if (env.getObject("ssf") == null) {
			env.putObject("ssf", new JsonObject());
		}
	}

	@Test
	void shouldExtractSessionRevokedEvent() {
		JsonObject eventData = new JsonObject();
		eventData.addProperty("initiating_entity", "policy");
		setUpSetToken(SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE, eventData);

		condition.execute(env);

		assertEquals(SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE, env.getString("ssf", "caep_event.type"));
	}

	@Test
	void shouldExtractCredentialChangeEvent() {
		JsonObject eventData = new JsonObject();
		eventData.addProperty("credential_type", "password");
		eventData.addProperty("change_type", "create");
		setUpSetToken(SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE, eventData);

		condition.execute(env);

		assertEquals(SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE, env.getString("ssf", "caep_event.type"));
	}

	@Test
	void shouldExtractDeviceComplianceChangeEvent() {
		JsonObject eventData = new JsonObject();
		eventData.addProperty("current_status", "not-compliant");
		eventData.addProperty("previous_status", "compliant");
		setUpSetToken(SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE, eventData);

		condition.execute(env);

		assertEquals(SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE, env.getString("ssf", "caep_event.type"));
	}

	@Test
	void shouldSkipSsfVerificationEvent() {
		JsonObject eventData = new JsonObject();
		eventData.addProperty("state", "some-state");
		setUpSetToken(SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE, eventData);

		condition.execute(env);

		assertNull(env.getString("ssf", "caep_event.type"));
	}

	@Test
	void shouldFailOnUnrecognizedEventType() {
		JsonObject eventData = new JsonObject();
		setUpSetToken("https://example.com/unknown-event", eventData);

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailOnEmptyEvents() {
		JsonObject claims = new JsonObject();
		claims.add("events", new JsonObject());

		JsonObject setToken = new JsonObject();
		setToken.add("claims", claims);
		env.putObject("set_token", setToken);
		env.putObject("ssf", new JsonObject());

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldClearPreviousCaepEventData() {
		// Set up initial CAEP event data
		JsonObject eventData = new JsonObject();
		eventData.addProperty("initiating_entity", "policy");
		setUpSetToken(SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE, eventData);
		condition.execute(env);
		assertEquals(SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE, env.getString("ssf", "caep_event.type"));

		// Now extract an SSF event — previous CAEP data should be cleared
		JsonObject verificationData = new JsonObject();
		verificationData.addProperty("state", "some-state");
		setUpSetToken(SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE, verificationData);
		condition.execute(env);

		assertNull(env.getString("ssf", "caep_event.type"));
	}
}
