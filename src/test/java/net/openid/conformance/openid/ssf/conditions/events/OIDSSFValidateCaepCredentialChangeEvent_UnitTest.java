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
public class OIDSSFValidateCaepCredentialChangeEvent_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFValidateCaepCredentialChangeEvent condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFValidateCaepCredentialChangeEvent();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void setUpCaepEvent(JsonObject eventData) {
		JsonObject ssf = new JsonObject();
		ssf.add("caep_event", new JsonObject());
		ssf.getAsJsonObject("caep_event").add("data", eventData);
		env.putObject("ssf", ssf);
	}

	@Test
	void shouldPassWithValidRequiredFields() {
		JsonObject data = new JsonObject();
		data.addProperty("credential_type", "password");
		data.addProperty("change_type", "create");
		setUpCaepEvent(data);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWithAllStandardCredentialTypes() {
		for (String type : new String[]{"password", "pin", "x509", "fido2-platform", "fido2-roaming",
			"fido-u2f", "verifiable-credential", "phone-voice", "phone-sms", "app"}) {
			JsonObject data = new JsonObject();
			data.addProperty("credential_type", type);
			data.addProperty("change_type", "create");
			setUpCaepEvent(data);
			assertDoesNotThrow(() -> condition.execute(env));
		}
	}

	@Test
	void shouldPassWithAllStandardChangeTypes() {
		for (String type : new String[]{"create", "revoke", "update", "delete"}) {
			JsonObject data = new JsonObject();
			data.addProperty("credential_type", "password");
			data.addProperty("change_type", type);
			setUpCaepEvent(data);
			assertDoesNotThrow(() -> condition.execute(env));
		}
	}

	@Test
	void shouldNotFailWithExtensionCredentialType() {
		// Non-standard credential_type values are allowed as extension values (logged, not error)
		JsonObject data = new JsonObject();
		data.addProperty("credential_type", "custom-hardware-token");
		data.addProperty("change_type", "create");
		setUpCaepEvent(data);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWhenCredentialTypeIsMissing() {
		JsonObject data = new JsonObject();
		data.addProperty("change_type", "create");
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenChangeTypeIsMissing() {
		JsonObject data = new JsonObject();
		data.addProperty("credential_type", "password");
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenCredentialTypeIsNotString() {
		JsonObject data = new JsonObject();
		data.addProperty("credential_type", 42);
		data.addProperty("change_type", "create");
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenChangeTypeIsNotString() {
		JsonObject data = new JsonObject();
		data.addProperty("credential_type", "password");
		data.addProperty("change_type", true);
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
