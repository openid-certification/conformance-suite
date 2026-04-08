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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFValidateCaepCommonOptionalFields_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private OIDSSFValidateCaepCommonOptionalFields condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFValidateCaepCommonOptionalFields();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void setUpCaepEvent(JsonObject eventData) {
		JsonObject ssf = new JsonObject();
		ssf.add("caep_event", new JsonObject());
		ssf.getAsJsonObject("caep_event").add("data", eventData);
		ssf.getAsJsonObject("caep_event").addProperty("type", SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE);
		env.putObject("ssf", ssf);
	}

	@Test
	void shouldPassWithNoOptionalFields() {
		setUpCaepEvent(new JsonObject());
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWithValidEventTimestamp() {
		JsonObject data = new JsonObject();
		data.addProperty("event_timestamp", 1700000000L);
		setUpCaepEvent(data);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWithStringEventTimestamp() {
		JsonObject data = new JsonObject();
		data.addProperty("event_timestamp", "not-a-number");
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldPassWithValidInitiatingEntity() {
		for (String entity : new String[]{"admin", "user", "policy", "system"}) {
			JsonObject data = new JsonObject();
			data.addProperty("initiating_entity", entity);
			setUpCaepEvent(data);
			assertDoesNotThrow(() -> condition.execute(env));
		}
	}

	@Test
	void shouldFailWithInvalidInitiatingEntity() {
		JsonObject data = new JsonObject();
		data.addProperty("initiating_entity", "unknown");
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldPassWithValidReasonAdmin() {
		JsonObject data = new JsonObject();
		JsonObject reason = new JsonObject();
		reason.addProperty("en", "Policy violation");
		data.add("reason_admin", reason);
		setUpCaepEvent(data);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWithStringReasonAdmin() {
		JsonObject data = new JsonObject();
		data.addProperty("reason_admin", "not-an-object");
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldPassWithValidReasonUser() {
		JsonObject data = new JsonObject();
		JsonObject reason = new JsonObject();
		reason.addProperty("en", "Your device is no longer compliant.");
		data.add("reason_user", reason);
		setUpCaepEvent(data);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWithStringReasonUser() {
		JsonObject data = new JsonObject();
		data.addProperty("reason_user", "not-an-object");
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWithEmptyReasonAdmin() {
		JsonObject data = new JsonObject();
		data.add("reason_admin", new JsonObject());
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWithNonStringReasonAdminValue() {
		JsonObject data = new JsonObject();
		JsonObject reason = new JsonObject();
		reason.addProperty("en", 123);
		data.add("reason_admin", reason);
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWithEmptyReasonUser() {
		JsonObject data = new JsonObject();
		data.add("reason_user", new JsonObject());
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWithNonStringReasonUserValue() {
		JsonObject data = new JsonObject();
		JsonObject reason = new JsonObject();
		reason.addProperty("en", true);
		data.add("reason_user", reason);
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldPassWithAllValidOptionalFields() {
		JsonObject data = new JsonObject();
		data.addProperty("event_timestamp", 1700000000L);
		data.addProperty("initiating_entity", "policy");
		JsonObject reasonAdmin = new JsonObject();
		reasonAdmin.addProperty("en", "Policy Violation: C076E822");
		data.add("reason_admin", reasonAdmin);
		JsonObject reasonUser = new JsonObject();
		reasonUser.addProperty("en", "This device is no longer compliant.");
		data.add("reason_user", reasonUser);
		setUpCaepEvent(data);
		assertDoesNotThrow(() -> condition.execute(env));
	}
}
