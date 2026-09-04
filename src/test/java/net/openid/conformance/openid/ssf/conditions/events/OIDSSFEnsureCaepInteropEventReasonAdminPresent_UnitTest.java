package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
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
public class OIDSSFEnsureCaepInteropEventReasonAdminPresent_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureCaepInteropEventReasonAdminPresent createCondition() {
		OIDSSFEnsureCaepInteropEventReasonAdminPresent condition = new OIDSSFEnsureCaepInteropEventReasonAdminPresent();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepareEventData(JsonObject eventData) {
		JsonObject caepEvent = new JsonObject();
		caepEvent.addProperty("type", SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE);
		caepEvent.add("data", eventData);
		JsonObject ssf = new JsonObject();
		ssf.add("caep_event", caepEvent);
		env.putObject("ssf", ssf);
	}

	@Test
	void passesWhenReasonAdminIsNonEmptyObject() {
		JsonObject reasonAdmin = new JsonObject();
		reasonAdmin.addProperty("en", "Policy Violation: C076E822");
		JsonObject eventData = new JsonObject();
		eventData.add("reason_admin", reasonAdmin);
		prepareEventData(eventData);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void failsWhenReasonAdminIsMissing() {
		prepareEventData(new JsonObject());
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsWhenReasonAdminIsEmptyObject() {
		JsonObject eventData = new JsonObject();
		eventData.add("reason_admin", new JsonObject());
		prepareEventData(eventData);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsWhenReasonAdminIsNotAnObject() {
		JsonObject eventData = new JsonObject();
		eventData.addProperty("reason_admin", "not an object");
		prepareEventData(eventData);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}
}
