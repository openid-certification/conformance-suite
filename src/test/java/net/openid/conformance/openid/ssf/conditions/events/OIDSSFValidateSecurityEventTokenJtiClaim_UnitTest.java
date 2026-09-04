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
public class OIDSSFValidateSecurityEventTokenJtiClaim_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFValidateSecurityEventTokenJtiClaim createCondition() {
		OIDSSFValidateSecurityEventTokenJtiClaim condition = new OIDSSFValidateSecurityEventTokenJtiClaim();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	@BeforeEach
	void setUp() {
		env.putObject("ssf", new JsonObject());
	}

	private void prepareSetToken(String jti, String eventMarker, String tokenValue) {
		JsonObject claims = new JsonObject();
		if (jti != null) {
			claims.addProperty("jti", jti);
		}
		claims.addProperty("evt", eventMarker);
		JsonObject setToken = new JsonObject();
		setToken.add("claims", claims);
		setToken.addProperty("value", tokenValue);
		env.putObject("set_token", setToken);
	}

	@Test
	void passesWhenJtiIsPresent() {
		prepareSetToken("jti-1", "e1", "token-1");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void failsWhenJtiIsMissing() {
		prepareSetToken(null, "e1", "token-1");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsWhenJtiIsBlank() {
		prepareSetToken("  ", "e1", "token-1");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void allowsRedeliveryOfIdenticalClaims() {
		// RFC 8935/8936: unacknowledged SETs may be redelivered - same jti, same claims.
		prepareSetToken("jti-1", "e1", "token-1");
		assertDoesNotThrow(() -> createCondition().execute(env));
		prepareSetToken("jti-1", "e1", "token-1");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void allowsRedeliveryWhenTokenWasReSigned() {
		// a transmitter may re-sign/re-serialize an unacknowledged SET on redelivery:
		// the serialized JWS differs, the claims (and jti) do not
		prepareSetToken("jti-1", "e1", "token-1");
		assertDoesNotThrow(() -> createCondition().execute(env));
		prepareSetToken("jti-1", "e1", "token-RESIGNED");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void failsWhenJtiIsReusedWithDifferentClaims() {
		// RFC 8417 2.2: jti MUST be unique within an event feed.
		prepareSetToken("jti-1", "e1", "token-1");
		assertDoesNotThrow(() -> createCondition().execute(env));
		prepareSetToken("jti-1", "e2-DIFFERENT", "token-2");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void distinctJtisPass() {
		prepareSetToken("jti-1", "e1", "token-1");
		assertDoesNotThrow(() -> createCondition().execute(env));
		prepareSetToken("jti-2", "e2", "token-2");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}
}
