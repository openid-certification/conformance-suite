package net.openid.conformance.vci10wallet.condition.clientattestation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateClientAttestationIssuedAt_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateClientAttestationIssuedAt cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateClientAttestationIssuedAt();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void putAttestationWithIat(Long iat) {
		JsonObject claims = new JsonObject();
		if (iat != null) {
			claims.addProperty("iat", iat);
		}
		JsonObject attestation = new JsonObject();
		attestation.add("claims", claims);
		env.putObject("client_attestation_object", attestation);
	}

	@Test
	public void testEvaluate_recentIatPasses() {
		putAttestationWithIat(Instant.now().getEpochSecond());
		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingIatIsAllowed() {
		// 'iat' is OPTIONAL per §5.1 — condition should skip without failing
		putAttestationWithIat(null);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_iatInFutureFails() {
		// 10 minutes in the future — beyond the 5-minute skew tolerance
		putAttestationWithIat(Instant.now().plusSeconds(10 * 60).getEpochSecond());
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_iatUnreasonablyInPastFails() {
		// Millisecond timestamp interpreted as seconds; also predates JWTUtil's min-reasonable floor
		putAttestationWithIat(1L);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingClientAttestationObjectFails() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
