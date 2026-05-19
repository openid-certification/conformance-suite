package net.openid.conformance.vci10wallet.condition;

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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateKeyAttestationExp_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateKeyAttestationExp cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateKeyAttestationExp();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putKeyAttestationExp(Long exp) {
		JsonObject claims = new JsonObject();
		if (exp != null) {
			claims.addProperty("exp", exp);
		}
		JsonObject keyAttestationJwt = new JsonObject();
		keyAttestationJwt.add("claims", claims);
		env.putObject("vci", "key_attestation_jwt", keyAttestationJwt);
	}

	@Test
	public void passesWhenExpIsAbsent() {
		putKeyAttestationExp(null);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void passesWhenExpIsInNearFuture() {
		putKeyAttestationExp(Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenExpIsInPast() {
		putKeyAttestationExp(Instant.now().minus(1, ChronoUnit.HOURS).getEpochSecond());

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
	}

	@Test
	public void allowsSmallClockSkewIntoPast() {
		putKeyAttestationExp(Instant.now().minus(1, ChronoUnit.MINUTES).getEpochSecond());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenExpIsUnreasonablyFarInFuture() {
		putKeyAttestationExp(Instant.now().plus(60L * 365L, ChronoUnit.DAYS).getEpochSecond());

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
	}
}
