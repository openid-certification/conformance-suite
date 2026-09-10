package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFWarnStreamUpdatedEventUnknownMembers_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFWarnStreamUpdatedEventUnknownMembers condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFWarnStreamUpdatedEventUnknownMembers();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
	}

	private static JsonObject specExampleClaims() {
		// SSF 1.0 Figure 47
		return JsonParser.parseString("""
			{
				"jti": "123456",
				"iss": "https://transmitter.example.com",
				"aud": "receiver.example.com",
				"iat": 1493856000,
				"sub_id": {
					"format": "opaque",
					"id" : "f67e39a0a4d34d56b3aa1bc4cff0069f"
				},
				"events": {
					"https://schemas.openid.net/secevent/ssf/event-type/stream-updated": {
						"status": "paused",
						"reason": "Internal error"
					}
				}
			}""").getAsJsonObject();
	}

	private static JsonObject streamUpdatedEvent(JsonObject claims) {
		return claims.getAsJsonObject("events").getAsJsonObject(SsfEvents.SSF_STREAM_UPDATED_EVENT_TYPE);
	}

	private void setUpSetToken(JsonObject claims) {
		JsonObject token = new JsonObject();
		token.add("claims", claims);
		env.putObject("set_token", token);
	}

	@Test
	void shouldPassWithSpecExample() {
		setUpSetToken(specExampleClaims());
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWithStatusOnly() {
		JsonObject claims = specExampleClaims();
		streamUpdatedEvent(claims).remove("reason");
		setUpSetToken(claims);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWhenStreamUpdatedEventIsAbsent() {
		// Presence is checked by OIDSSFValidateStreamUpdatedEvent, not here
		JsonObject claims = specExampleClaims();
		claims.remove("events");
		setUpSetToken(claims);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWithUnknownMember() {
		JsonObject claims = specExampleClaims();
		streamUpdatedEvent(claims).addProperty("reasn", "typo");
		setUpSetToken(claims);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("does not define"));
	}
}
