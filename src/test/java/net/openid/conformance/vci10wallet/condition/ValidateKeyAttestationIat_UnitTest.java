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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateKeyAttestationIat_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateKeyAttestationIat cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateKeyAttestationIat();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putKeyAttestationIat(Long iat) {
		JsonObject claims = new JsonObject();
		if (iat != null) {
			claims.addProperty("iat", iat);
		}
		JsonObject keyAttestationJwt = new JsonObject();
		keyAttestationJwt.add("claims", claims);
		env.putObject("vci", "key_attestation_jwt", keyAttestationJwt);
	}

	@Test
	public void passesWhenIatIsRecent() {
		putKeyAttestationIat(Instant.now().minus(1, ChronoUnit.HOURS).getEpochSecond());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenIatIsMissing() {
		putKeyAttestationIat(null);

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
		assertTrue(env.getString("vci", "credential_error_response.body.error_description")
			.contains("REQUIRED 'iat'"));
	}

	@Test
	public void failsWhenIatIsInTheFuture() {
		putKeyAttestationIat(Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond());

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
	}

	@Test
	public void allowsSmallClockSkewIntoFuture() {
		putKeyAttestationIat(Instant.now().plus(1, ChronoUnit.MINUTES).getEpochSecond());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenIatIsBeforeJwtSpecEra() {
		putKeyAttestationIat(Instant.parse("2010-06-15T00:00:00Z").getEpochSecond());

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
	}
}
