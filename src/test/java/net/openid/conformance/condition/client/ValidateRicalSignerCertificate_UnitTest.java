package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateRicalSignerCertificate_UnitTest {

	private ValidateRicalSignerCertificate cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private RicalTestFixtures.ReaderPki pki;

	@BeforeEach
	public void setUp() {
		cond = new ValidateRicalSignerCertificate();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
		pki = RicalTestFixtures.generateReaderPki();
	}

	@Test
	public void testEvaluate_passesForValidSigner() {
		RicalTestFixtures.putRical(env, RicalTestFixtures.goodSignedRical(List.of(pki.getCaCert())));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsForExpiredSigner() {
		RicalTestFixtures.RicalSigner expired = RicalTestFixtures.generateSigner(
			RicalTestFixtures.past(), RicalTestFixtures.past());
		RicalTestFixtures.putRical(env,
			RicalTestFixtures.goodSignedRical(List.of(pki.getCaCert()), expired));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("validity"), e.getMessage());
	}
}
