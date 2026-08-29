package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateOwnMdocSigningChainAgainstVical_UnitTest {

	private ValidateOwnMdocSigningChainAgainstVical cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ValidateOwnMdocSigningChainAgainstVical();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	@Test
	public void testEvaluate_passesWhenSuiteIacaListed() {
		VicalTestFixtures.putVical(env, VicalTestFixtures.goodSignedVical(
			List.of(TestKeysAndCerts.getIacaRootCert())));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenSuiteIacaNotListed() {
		VicalTestFixtures.IssuerPki otherPki = VicalTestFixtures.generateIssuerPki();
		VicalTestFixtures.putVical(env, VicalTestFixtures.goodSignedVical(List.of(otherPki.getIacaCert())));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not chain"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenVicalSignatureBroken() {
		byte[] vical = VicalTestFixtures.goodSignedVical(List.of(TestKeysAndCerts.getIacaRootCert()));
		vical[vical.length / 2] ^= 0x01;
		VicalTestFixtures.putVical(env, vical);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("could not be parsed or its COSE signature"), e.getMessage());
	}
}
