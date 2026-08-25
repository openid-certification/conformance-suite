package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
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
public class OIDSSFEnsureCaepInteropEventSubjectFormat_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureCaepInteropEventSubjectFormat condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFEnsureCaepInteropEventSubjectFormat();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	static void setUpCaepEvent(Environment env, JsonElement subId) {
		JsonObject claims = new JsonObject();
		if (subId != null) {
			claims.add("sub_id", subId);
		}
		JsonObject token = new JsonObject();
		token.add("claims", claims);
		env.putObject("set_token", token);

		JsonObject caepEvent = new JsonObject();
		caepEvent.addProperty("type", SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE);
		caepEvent.add("data", new JsonObject());
		JsonObject ssf = new JsonObject();
		ssf.add("caep_event", caepEvent);
		env.putObject("ssf", ssf);
	}

	@Test
	void shouldPassWithEmailSubject() {
		setUpCaepEvent(env, JsonParser.parseString("{\"format\":\"email\",\"email\":\"user@example.com\"}"));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWithIssSubSubject() {
		setUpCaepEvent(env, JsonParser.parseString("{\"format\":\"iss_sub\",\"iss\":\"https://idp.example.com\",\"sub\":\"1234\"}"));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassThroughComplexSubject() {
		// complex subjects are judged by OIDSSFWarnCaepInteropEventUsesComplexSubject
		setUpCaepEvent(env, JsonParser.parseString("{\"format\":\"complex\",\"user\":{\"format\":\"email\",\"email\":\"user@example.com\"}}"));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWithOpaqueSubjectAndMentionVerificationEvent() {
		setUpCaepEvent(env, JsonParser.parseString("{\"format\":\"opaque\",\"id\":\"abc\"}"));
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("Verification event"));
	}

	@Test
	void shouldFailWithUnsupportedFormat() {
		setUpCaepEvent(env, JsonParser.parseString("{\"format\":\"phone_number\",\"phone_number\":\"+12065550100\"}"));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWithSsfAdditionalFormat() {
		setUpCaepEvent(env, JsonParser.parseString("{\"format\":\"jwt_id\",\"iss\":\"https://idp.example.com\",\"jti\":\"abc\"}"));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenSubIdIsMissing() {
		setUpCaepEvent(env, null);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenFormatIsMissing() {
		setUpCaepEvent(env, JsonParser.parseString("{\"email\":\"user@example.com\"}"));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
