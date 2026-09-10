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

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureSecurityEventValuesAreJsonObjects_UnitTest {

	private static final String VERIFICATION = "https://schemas.openid.net/secevent/ssf/event-type/verification";
	private static final String SESSION_REVOKED = "https://schemas.openid.net/secevent/caep/event-type/session-revoked";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureSecurityEventValuesAreJsonObjects condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFEnsureSecurityEventValuesAreJsonObjects();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	private void setClaims(String claimsJson) {
		JsonObject setToken = new JsonObject();
		setToken.add("claims", JsonParser.parseString(claimsJson));
		env.putObject("set_token", setToken);
	}

	@Test
	void passesForObjectValue() {
		setClaims("{\"events\":{\"" + VERIFICATION + "\":{\"state\":\"abc\"}}}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesForEmptyObjectValue() {
		setClaims("{\"events\":{\"" + SESSION_REVOKED + "\":{}}}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void failsForNullValue() {
		setClaims("{\"events\":{\"" + SESSION_REVOKED + "\":null}}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsForStringValue() {
		setClaims("{\"events\":{\"" + VERIFICATION + "\":\"abc\"}}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsForArrayValue() {
		setClaims("{\"events\":{\"" + SESSION_REVOKED + "\":[{}]}}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsWhenEventsIsNotAnObject() {
		setClaims("{\"events\":[\"" + SESSION_REVOKED + "\"]}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsWhenEventsIsMissing() {
		setClaims("{\"iss\":\"https://transmitter.example\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
