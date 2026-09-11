package net.openid.conformance.openid.ssf.conditions.events;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
	void shouldPassWithSpecExample() {
		// CAEP 1.0 Figure 9
		setUpCaepEvent(JsonParser.parseString("""
			{
				"credential_type": "fido2-roaming",
				"change_type": "create",
				"fido2_aaguid": "accced6a-63f5-490a-9eea-e59bc1896cfc",
				"friendly_name": "Jane's USB authenticator",
				"initiating_entity": "user",
				"reason_admin": {
					"en": "User self-enrollment"
				},
				"event_timestamp": 1615304991
			}""").getAsJsonObject());
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldNotFailWithExtensionCredentialType() {
		// CAEP 1.0 3.3.1 permits "any other credential type supported mutually by the Transmitter and the Receiver"
		JsonObject data = new JsonObject();
		data.addProperty("credential_type", "custom-hardware-token");
		data.addProperty("change_type", "create");
		setUpCaepEvent(data);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWithNonStandardChangeType() {
		// CAEP 1.0 3.3.1: change_type "MUST be one of" create, revoke, update, delete
		JsonObject data = new JsonObject();
		data.addProperty("credential_type", "password");
		data.addProperty("change_type", "suspend");
		setUpCaepEvent(data);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("change_type"));
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
	void shouldPassWithOptionalStringFields() {
		JsonObject data = new JsonObject();
		data.addProperty("credential_type", "x509");
		data.addProperty("change_type", "create");
		data.addProperty("friendly_name", "Jane's laptop certificate");
		data.addProperty("x509_issuer", "CN=Example CA");
		data.addProperty("x509_serial", "0x1A2B3C");
		data.addProperty("fido2_aaguid", "accced6a-63f5-490a-9eea-e59bc1896cfc");
		setUpCaepEvent(data);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWhenAnOptionalStringFieldIsNotString() {
		// CAEP 1.0 3.3.1: x509_serial is "OPTIONAL, JSON string"
		JsonObject data = new JsonObject();
		data.addProperty("credential_type", "x509");
		data.addProperty("change_type", "create");
		data.addProperty("x509_serial", 123456);
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
