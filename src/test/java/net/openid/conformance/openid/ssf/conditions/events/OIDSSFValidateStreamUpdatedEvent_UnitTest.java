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
public class OIDSSFValidateStreamUpdatedEvent_UnitTest {

	private static final String STREAM_ID = "f67e39a0a4d34d56b3aa1bc4cff0069f";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFValidateStreamUpdatedEvent condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFValidateStreamUpdatedEvent();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);

		JsonObject ssf = new JsonObject();
		JsonObject stream = new JsonObject();
		stream.addProperty("stream_id", STREAM_ID);
		ssf.add("stream", stream);
		env.putObject("ssf", ssf);
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
	void shouldPassWithAllStreamStatuses() {
		for (String status : new String[]{"enabled", "paused", "disabled"}) {
			JsonObject claims = specExampleClaims();
			streamUpdatedEvent(claims).addProperty("status", status);
			setUpSetToken(claims);
			assertDoesNotThrow(() -> condition.execute(env));
		}
	}

	@Test
	void shouldPassWithoutReason() {
		JsonObject claims = specExampleClaims();
		streamUpdatedEvent(claims).remove("reason");
		setUpSetToken(claims);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWithUnknownEventMembers() {
		// Unknown members are flagged by OIDSSFWarnStreamUpdatedEventUnknownMembers, not here
		JsonObject claims = specExampleClaims();
		streamUpdatedEvent(claims).addProperty("extra", "value");
		setUpSetToken(claims);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWhenEventsClaimIsMissing() {
		JsonObject claims = specExampleClaims();
		claims.remove("events");
		setUpSetToken(claims);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("events"));
	}

	@Test
	void shouldFailWhenStreamUpdatedEventIsAbsent() {
		JsonObject claims = specExampleClaims();
		claims.getAsJsonObject("events").remove(SsfEvents.SSF_STREAM_UPDATED_EVENT_TYPE);
		claims.getAsJsonObject("events").add(SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE, new JsonObject());
		setUpSetToken(claims);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("stream-updated"));
	}

	@Test
	void shouldFailWhenStreamUpdatedEventIsNotObject() {
		JsonObject claims = specExampleClaims();
		claims.getAsJsonObject("events").addProperty(SsfEvents.SSF_STREAM_UPDATED_EVENT_TYPE, "paused");
		setUpSetToken(claims);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenStatusIsMissing() {
		JsonObject claims = specExampleClaims();
		streamUpdatedEvent(claims).remove("status");
		setUpSetToken(claims);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("status"));
	}

	@Test
	void shouldFailWhenStatusIsNotString() {
		JsonObject claims = specExampleClaims();
		streamUpdatedEvent(claims).addProperty("status", 1);
		setUpSetToken(claims);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenStatusIsNotKnownValue() {
		JsonObject claims = specExampleClaims();
		streamUpdatedEvent(claims).addProperty("status", "stopped");
		setUpSetToken(claims);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("status"));
	}

	@Test
	void shouldFailWhenReasonIsNotString() {
		JsonObject claims = specExampleClaims();
		streamUpdatedEvent(claims).add("reason", new JsonObject());
		setUpSetToken(claims);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("reason"));
	}

	@Test
	void shouldFailWhenSubIdIsMissing() {
		JsonObject claims = specExampleClaims();
		claims.remove("sub_id");
		setUpSetToken(claims);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("sub_id"));
	}

	@Test
	void shouldFailWhenSubIdIsNotObject() {
		JsonObject claims = specExampleClaims();
		claims.addProperty("sub_id", STREAM_ID);
		setUpSetToken(claims);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenSubIdFormatIsMissing() {
		JsonObject claims = specExampleClaims();
		claims.getAsJsonObject("sub_id").remove("format");
		setUpSetToken(claims);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenSubIdFormatIsNotOpaque() {
		JsonObject claims = specExampleClaims();
		claims.getAsJsonObject("sub_id").addProperty("format", "email");
		setUpSetToken(claims);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("opaque"));
	}

	@Test
	void shouldFailWhenSubIdIdIsMissing() {
		JsonObject claims = specExampleClaims();
		claims.getAsJsonObject("sub_id").remove("id");
		setUpSetToken(claims);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenSubIdIdDoesNotMatchStreamId() {
		JsonObject claims = specExampleClaims();
		claims.getAsJsonObject("sub_id").addProperty("id", "some-other-stream");
		setUpSetToken(claims);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("unique ID of the stream"));
	}

	@Test
	void shouldFailWhenStreamIdIsNotInEnvironment() {
		env.getObject("ssf").remove("stream");
		setUpSetToken(specExampleClaims());
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
