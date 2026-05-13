package net.openid.conformance.condition.as.clientattestation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateClientAttestationNotBefore_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateClientAttestationNotBefore cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateClientAttestationNotBefore();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void putAttestationWithNbf(Long nbf) {
		JsonObject claims = new JsonObject();
		if (nbf != null) {
			claims.addProperty("nbf", nbf);
		}
		JsonObject attestation = new JsonObject();
		attestation.add("claims", claims);
		env.putObject("client_attestation_object", attestation);
	}

	@Test
	public void testEvaluate_nbfInPastPasses() {
		putAttestationWithNbf(Instant.now().minusSeconds(60).getEpochSecond());
		cond.execute(env);
	}

	@Test
	public void testEvaluate_nbfJustAheadWithinSkewPasses() {
		// 60s in the future — within the 5-minute skew tolerance
		putAttestationWithNbf(Instant.now().plusSeconds(60).getEpochSecond());
		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingNbfIsAllowed() {
		// 'nbf' is OPTIONAL per §5.1 — condition should skip without failing
		putAttestationWithNbf(null);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_nbfBeyondSkewInFutureFails() {
		// 10 minutes in the future — beyond the 5-minute skew tolerance
		putAttestationWithNbf(Instant.now().plusSeconds(10 * 60).getEpochSecond());
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_nbfUnreasonablyInPastFails() {
		// Predates JWTUtil's min-reasonable floor (2024) — catches epoch-default bugs
		putAttestationWithNbf(1L);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingClientAttestationObjectFails() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
