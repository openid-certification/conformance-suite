package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
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
public class OIDSSFValidateCaepDeviceComplianceChangeEvent_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFValidateCaepDeviceComplianceChangeEvent condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFValidateCaepDeviceComplianceChangeEvent();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void setUpCaepEvent(JsonObject eventData) {
		JsonObject ssf = new JsonObject();
		ssf.add("caep_event", new JsonObject());
		ssf.getAsJsonObject("caep_event").add("data", eventData);
		env.putObject("ssf", ssf);
	}

	@Test
	void shouldPassWithCompliantToNotCompliant() {
		JsonObject data = new JsonObject();
		data.addProperty("current_status", "not-compliant");
		data.addProperty("previous_status", "compliant");
		setUpCaepEvent(data);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWithNotCompliantToCompliant() {
		JsonObject data = new JsonObject();
		data.addProperty("current_status", "compliant");
		data.addProperty("previous_status", "not-compliant");
		setUpCaepEvent(data);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWhenCurrentStatusIsMissing() {
		JsonObject data = new JsonObject();
		data.addProperty("previous_status", "compliant");
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenPreviousStatusIsMissing() {
		JsonObject data = new JsonObject();
		data.addProperty("current_status", "not-compliant");
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWithInvalidCurrentStatus() {
		JsonObject data = new JsonObject();
		data.addProperty("current_status", "unknown");
		data.addProperty("previous_status", "compliant");
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWithInvalidPreviousStatus() {
		JsonObject data = new JsonObject();
		data.addProperty("current_status", "not-compliant");
		data.addProperty("previous_status", "unknown");
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenCurrentStatusIsNotString() {
		JsonObject data = new JsonObject();
		data.addProperty("current_status", 0);
		data.addProperty("previous_status", "compliant");
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
