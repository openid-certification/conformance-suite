package net.openid.conformance.vci10wallet.condition;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VCIEnsureJwtProofKidIsNotAnAttestedKeyIndex_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIEnsureJwtProofKidIsNotAnAttestedKeyIndex cond;

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureJwtProofKidIsNotAnAttestedKeyIndex();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	@Test
	public void testEvaluate_noKidIndexPasses() {
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_kidIndexIsReported() {
		env.putString(AbstractVCIValidateCredentialRequestProof.KID_ATTESTED_KEY_INDEX_ENV_KEY, "0");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
