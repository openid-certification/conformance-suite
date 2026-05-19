package net.openid.conformance.condition.client;

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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureNoUseAttestationChallengeErrorAfterServerIssuedChallenge_UnitTest {

	private static final String FLAG = ExtractClientAttestationChallengeFromResponseHeader.CHALLENGE_ISSUED_BY_SERVER_FLAG;

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureNoUseAttestationChallengeErrorAfterServerIssuedChallenge cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureNoUseAttestationChallengeErrorAfterServerIssuedChallenge();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void noServerIssuedChallengeYet_passesEvenWithError() {
		// First-attempt scenario where the AS hasn't issued any challenge yet — the
		// use_attestation_challenge error is the legitimate initial-bootstrap path.
		env.putString("par_endpoint_use_attestation_challenge_error", "use_attestation_challenge");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void serverIssuedChallengeButNoError_passes() {
		env.putString(FLAG, "true");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void serverIssuedChallengePlusParError_fails() {
		// AS supplied a challenge via challenge_endpoint or §8.1 header, the wallet used it, and the
		// AS rejected the PAR request with use_attestation_challenge anyway — AS bug.
		env.putString(FLAG, "true");
		env.putString("par_endpoint_use_attestation_challenge_error", "use_attestation_challenge");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void serverIssuedChallengePlusTokenError_fails() {
		env.putString(FLAG, "true");
		env.putString("token_endpoint_use_attestation_challenge_error", "use_attestation_challenge");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void flagSetToOtherValue_treatedAsNotSet() {
		// Defensive: only literal "true" trips the check.
		env.putString(FLAG, "false");
		env.putString("token_endpoint_use_attestation_challenge_error", "use_attestation_challenge");

		assertDoesNotThrow(() -> cond.execute(env));
	}
}
