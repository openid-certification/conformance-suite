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
public class OIDSSFValidateSecurityEventTokenAudClaim_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFValidateSecurityEventTokenAudClaim condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFValidateSecurityEventTokenAudClaim();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	private void setClaims(String claimsJson) {
		JsonObject setToken = new JsonObject();
		setToken.add("claims", JsonParser.parseString(claimsJson));
		env.putObject("set_token", setToken);
	}

	@Test
	void passesForStringAudience() {
		setClaims("{\"aud\":\"https://receiver.example\"}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesForArrayAudience() {
		setClaims("{\"aud\":[\"https://receiver.example\",\"urn:example:other\"]}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void failsWhenMissing() {
		setClaims("{\"iss\":\"https://transmitter.example\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsForEmptyString() {
		setClaims("{\"aud\":\"\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsForEmptyArray() {
		setClaims("{\"aud\":[]}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsForNonStringArrayMember() {
		setClaims("{\"aud\":[\"https://receiver.example\",42]}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsForNumber() {
		setClaims("{\"aud\":42}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void failsForObject() {
		setClaims("{\"aud\":{\"value\":\"https://receiver.example\"}}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
