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
public class ValidateClientAttestationExpiration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateClientAttestationExpiration cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateClientAttestationExpiration();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void putAttestationWithExp(long exp) {
		JsonObject claims = new JsonObject();
		claims.addProperty("exp", exp);
		JsonObject attestation = new JsonObject();
		attestation.add("claims", claims);
		env.putObject("client_attestation_object", attestation);
	}

	@Test
	public void testEvaluate_expInFuturePasses() {
		putAttestationWithExp(Instant.now().plusSeconds(5 * 60).getEpochSecond());
		cond.execute(env);
	}

	@Test
	public void testEvaluate_expJustWithinClockSkewPasses() {
		// 30s in the past — well within the JWTUtil 5-minute skew tolerance
		putAttestationWithExp(Instant.now().minusSeconds(30).getEpochSecond());
		cond.execute(env);
	}

	@Test
	public void testEvaluate_expBeyondClockSkewFails() {
		// 10 minutes in the past — beyond the 5-minute skew tolerance
		putAttestationWithExp(Instant.now().minusSeconds(10 * 60).getEpochSecond());
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_expUnreasonablyFarInFutureFails() {
		// Millisecond timestamp mistakenly used as seconds — ~54,000 years in the future
		putAttestationWithExp(Instant.now().toEpochMilli());
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingExpFails() {
		JsonObject claims = new JsonObject();
		JsonObject attestation = new JsonObject();
		attestation.add("claims", claims);
		env.putObject("client_attestation_object", attestation);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingClientAttestationObjectFails() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
