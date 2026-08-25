package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
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
public class OIDSSFValidateSecurityEventTokenSubIdClaim_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFValidateSecurityEventTokenSubIdClaim condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFValidateSecurityEventTokenSubIdClaim();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void setUpSetToken(JsonElement subId) {
		JsonObject claims = new JsonObject();
		claims.addProperty("iss", "https://transmitter.example.com");
		claims.addProperty("jti", "756E69717565206964656E746966696572");
		if (subId != null) {
			claims.add("sub_id", subId);
		}
		JsonObject token = new JsonObject();
		token.add("claims", claims);
		env.putObject("set_token", token);
	}

	@Test
	void shouldPassWithEmailSubject() {
		setUpSetToken(JsonParser.parseString("{\"format\":\"email\",\"email\":\"user@example.com\"}"));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWithIssSubSubject() {
		setUpSetToken(JsonParser.parseString("{\"format\":\"iss_sub\",\"iss\":\"https://idp.example.com\",\"sub\":\"1234\"}"));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWithOpaqueSubject() {
		setUpSetToken(JsonParser.parseString("{\"format\":\"opaque\",\"id\":\"stream-1\"}"));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWithComplexSubject() {
		setUpSetToken(JsonParser.parseString("""
			{"format":"complex",
			 "user":{"format":"email","email":"user@example.com"},
			 "device":{"format":"opaque","id":"device-1"}}"""));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWhenSubIdIsMissing() {
		setUpSetToken(null);
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("sub_id"));
	}

	@Test
	void shouldFailWhenSubIdIsNotAnObject() {
		setUpSetToken(JsonParser.parseString("\"user@example.com\""));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenSubIdHasNoFormat() {
		setUpSetToken(JsonParser.parseString("{\"email\":\"user@example.com\"}"));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenRequiredMemberIsMissing() {
		setUpSetToken(JsonParser.parseString("{\"format\":\"iss_sub\",\"iss\":\"https://idp.example.com\"}"));
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("'sub'"));
	}

	@Test
	void shouldFailWhenComplexSubjectMemberIsInvalid() {
		setUpSetToken(JsonParser.parseString("{\"format\":\"complex\",\"user\":{\"format\":\"email\"}}"));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
