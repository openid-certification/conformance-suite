package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureSecurityEventTokenIatIsNotInFuture_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureSecurityEventTokenIatIsNotInFuture createCondition() {
		OIDSSFEnsureSecurityEventTokenIatIsNotInFuture condition = new OIDSSFEnsureSecurityEventTokenIatIsNotInFuture();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepareIat(Long iat) {
		JsonObject claims = new JsonObject();
		if (iat != null) {
			claims.addProperty("iat", iat);
		}
		JsonObject setToken = new JsonObject();
		setToken.add("claims", claims);
		env.putObject("set_token", setToken);
	}

	@Test
	void currentIatIsValid() {
		prepareIat(Instant.now().getEpochSecond());
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void iatSlightlyAheadOfSuiteClockIsValid() {
		// a transmitter one second (or a minute) ahead of the suite's clock must pass
		prepareIat(Instant.now().plusSeconds(60).getEpochSecond());
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void iatBeyondClockSkewFails() {
		prepareIat(Instant.now().plusSeconds(10 * 60).getEpochSecond());
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void iatZeroFails() {
		prepareIat(0L);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void millisecondIatFails() {
		prepareIat(Instant.now().toEpochMilli());
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void missingIatFails() {
		prepareIat(null);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}
}
