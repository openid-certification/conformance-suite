package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFRecordSecurityEventTokenSubjectFormat_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFRecordSecurityEventTokenSubjectFormat condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFRecordSecurityEventTokenSubjectFormat();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putObject("ssf", new JsonObject());
	}

	static void setUpSetToken(Environment env, String subIdJson, String eventType) {
		JsonObject claims = new JsonObject();
		if (subIdJson != null) {
			claims.add("sub_id", JsonParser.parseString(subIdJson));
		}
		JsonObject events = new JsonObject();
		events.add(eventType, new JsonObject());
		claims.add("events", events);
		JsonObject token = new JsonObject();
		token.add("claims", claims);
		env.putObject("set_token", token);
	}

	private JsonObject observed() {
		JsonElement el = env.getElementFromObject("ssf", OIDSSFRecordSecurityEventTokenSubjectFormat.OBSERVED_SUBJECT_FORMATS_KEY);
		assertTrue(el != null && el.isJsonObject());
		return el.getAsJsonObject();
	}

	@Test
	void shouldRecordFormatCountAndEventTypes() {
		setUpSetToken(env, "{\"format\":\"email\",\"email\":\"user@example.com\"}", SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE);
		assertDoesNotThrow(() -> condition.execute(env));
		setUpSetToken(env, "{\"format\":\"email\",\"email\":\"other@example.com\"}", SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE);
		assertDoesNotThrow(() -> condition.execute(env));
		setUpSetToken(env, "{\"format\":\"opaque\",\"id\":\"stream-1\"}", SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE);
		assertDoesNotThrow(() -> condition.execute(env));

		JsonObject observed = observed();
		assertEquals(2, observed.size());

		JsonObject email = observed.getAsJsonObject("email");
		assertEquals(2, OIDFJSON.getInt(email.get("count")));
		assertEquals(List.of(SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE, SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE),
			OIDFJSON.convertJsonArrayToList(email.getAsJsonArray("event_types")));
		assertFalse(email.has("members"));

		JsonObject opaque = observed.getAsJsonObject("opaque");
		assertEquals(1, OIDFJSON.getInt(opaque.get("count")));
		assertEquals(List.of(SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE), OIDFJSON.convertJsonArrayToList(opaque.getAsJsonArray("event_types")));
	}

	@Test
	void shouldNotDuplicateEventTypes() {
		setUpSetToken(env, "{\"format\":\"email\",\"email\":\"user@example.com\"}", SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE);
		assertDoesNotThrow(() -> condition.execute(env));
		assertDoesNotThrow(() -> condition.execute(env));

		JsonObject email = observed().getAsJsonObject("email");
		assertEquals(2, OIDFJSON.getInt(email.get("count")));
		assertEquals(1, email.getAsJsonArray("event_types").size());
	}

	@Test
	void shouldRecordComplexSubjectMemberFormats() {
		setUpSetToken(env, """
			{"format":"complex",
			 "user":{"format":"email","email":"user@example.com"},
			 "device":{"format":"opaque","id":"device-1"}}""", SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE);
		assertDoesNotThrow(() -> condition.execute(env));
		setUpSetToken(env, """
			{"format":"complex",
			 "user":{"format":"iss_sub","iss":"https://idp.example.com","sub":"1"},
			 "device":{"format":"opaque","id":"device-2"}}""", SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE);
		assertDoesNotThrow(() -> condition.execute(env));

		JsonObject complex = observed().getAsJsonObject("complex");
		assertEquals(2, OIDFJSON.getInt(complex.get("count")));
		JsonObject members = complex.getAsJsonObject("members");
		assertEquals(List.of("email", "iss_sub"), OIDFJSON.convertJsonArrayToList(members.getAsJsonArray("user")));
		assertEquals(List.of("opaque"), OIDFJSON.convertJsonArrayToList(members.getAsJsonArray("device")));
	}

	@Test
	void shouldIgnoreSetWithoutSubId() {
		setUpSetToken(env, null, SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE);
		assertDoesNotThrow(() -> condition.execute(env));
		assertNull(env.getElementFromObject("ssf", OIDSSFRecordSecurityEventTokenSubjectFormat.OBSERVED_SUBJECT_FORMATS_KEY));
	}
}
