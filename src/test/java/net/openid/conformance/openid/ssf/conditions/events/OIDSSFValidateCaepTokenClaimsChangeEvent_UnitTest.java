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
public class OIDSSFValidateCaepTokenClaimsChangeEvent_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFValidateCaepTokenClaimsChangeEvent condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFValidateCaepTokenClaimsChangeEvent();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	private void setUpCaepEvent(JsonObject eventData) {
		JsonObject ssf = new JsonObject();
		ssf.add("caep_event", new JsonObject());
		ssf.getAsJsonObject("caep_event").add("data", eventData);
		env.putObject("ssf", ssf);
	}

	@Test
	void shouldPassWithSpecExampleRequiredClaimsOnly() {
		// CAEP 1.0 Figure 6
		setUpCaepEvent(JsonParser.parseString("""
			{
				"event_timestamp": 1615304991,
				"claims": {
					"role": "ro-admin"
				}
			}""").getAsJsonObject());
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWithSpecExampleOptionalClaims() {
		// CAEP 1.0 Figure 7
		setUpCaepEvent(JsonParser.parseString("""
			{
				"event_timestamp": 1615304991,
				"initiating_entity": "policy",
				"reason_admin": {
					"en": "User left trusted network: CorpNet3"
				},
				"reason_user": {
					"en": "You're no longer connected to a trusted network.",
					"it": "Non sei piu connesso a una rete attendibile."
				},
				"claims": {
					"trusted_network": false
				}
			}""").getAsJsonObject());
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWithSamlClaimUri() {
		// CAEP 1.0 Figure 8
		setUpCaepEvent(JsonParser.parseString("""
			{
				"event_timestamp": 1615304991,
				"claims": {
					"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role": "ro-admin"
				}
			}""").getAsJsonObject());
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWhenClaimsIsMissing() {
		JsonObject data = new JsonObject();
		data.addProperty("event_timestamp", 1615304991);
		setUpCaepEvent(data);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("claims"));
	}

	@Test
	void shouldFailWhenClaimsIsEmptyObject() {
		JsonObject data = new JsonObject();
		data.add("claims", new JsonObject());
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenClaimsIsString() {
		JsonObject data = new JsonObject();
		data.addProperty("claims", "role=ro-admin");
		setUpCaepEvent(data);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenClaimsIsArray() {
		setUpCaepEvent(JsonParser.parseString("{\"claims\": [{\"role\": \"ro-admin\"}]}").getAsJsonObject());
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
